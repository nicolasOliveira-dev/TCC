package com.example.EduGrid.solver.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.ProfessorRepository;
import com.example.EduGrid.service.BlocoDisciplinaService;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.GrupoSincronizacao;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;
import com.example.EduGrid.solver.domain.VinculoSincronizacao;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

@Service
public class OtimizacaoService {

    private final BlocoDisciplinaService blocoDisciplinaService;
    private final IdentificacaoSincronizacaoService sincronizacaoService;
    private final HorarioRepository horarioRepository;
    private final ProfessorRepository professorRepository;
    private final SolverManager<GradePlanejamento> solverManager;
    private final PersistenciaGradeService persistenciaGradeService;
    @Value("${edugrid.solver.spent-limit:2m}")
    private Duration spentLimit = Duration.ofMinutes(2);
    @Value("${edugrid.solver.random-seed:}")
    private String configuredSeed = "";
    private final AtomicLong seedSequence = new AtomicLong(0L);

    public OtimizacaoService(
            BlocoDisciplinaService blocoDisciplinaService,
            IdentificacaoSincronizacaoService sincronizacaoService,
            HorarioRepository horarioRepository,
            ProfessorRepository professorRepository,
            SolverManager<GradePlanejamento> solverManager,
            PersistenciaGradeService persistenciaGradeService) {

        this.blocoDisciplinaService = blocoDisciplinaService;
        this.sincronizacaoService = sincronizacaoService;
        this.horarioRepository = horarioRepository;
        this.professorRepository = professorRepository;
        this.solverManager = solverManager;
        this.persistenciaGradeService = persistenciaGradeService;
    }

    public GradePlanejamento montarProblema(
            List<Turma> turmas,
            TamanhoGrade tamanhoGrade) {
        return montarProblema(turmas, tamanhoGrade, null, false);
    }

    public GradePlanejamento montarProblema(
            List<Turma> turmas,
            TamanhoGrade tamanhoGrade,
            GradeHoraria gradeOrigem,
            boolean preservarEdicoesManuais) {
        if (turmas == null || tamanhoGrade == null) {
            throw new IllegalArgumentException("Turmas e tamanho da grade são obrigatórios.");
        }

        List<AulaPlanejamento> aulas = new ArrayList<>();

        for (Turma turma : turmas) {
            if (turma == null || turma.getId() == null) {
                throw new IllegalArgumentException("Todas as turmas precisam ter um identificador.");
            }
            turma.setTamanhoGrade(tamanhoGrade);

            if (turma.getDisciplinas() == null) {
                continue;
            }

            for (Disciplina disciplina : turma.getDisciplinas()) {

                if (disciplina == null) {
                    continue;
                }
                if (disciplina.getId() == null) {
                    throw new IllegalArgumentException("Todas as disciplinas precisam ter um identificador.");
                }

                List<Integer> blocos =
                        blocoDisciplinaService.calcularBlocos(
                                disciplina,
                                tamanhoGrade
                        );

                for (int indice = 0; indice < blocos.size(); indice++) {

                    AulaPlanejamento aula = new AulaPlanejamento();

                    aula.setId(
                            turma.getId() + "-"
                            + disciplina.getId() + "-"
                            + indice
                    );

                    aula.setTurma(turma);
                    aula.setDisciplina(disciplina);
                    aula.setDuracaoPeriodos(blocos.get(indice));

                    aulas.add(aula);
                }
            }
        }

        List<Horario> horariosPersistidos = turmas.isEmpty()
                ? List.of()
                : gradeOrigem == null
                        ? horarioRepository.findByTurmaInAndGradeHorariaIsNull(turmas)
                        : horarioRepository.findByGradeHoraria(gradeOrigem);
        if (gradeOrigem != null && horariosPersistidos.isEmpty()) {
            horariosPersistidos = horarioRepository.findByTurmaInAndGradeHorariaIsNull(turmas);
        }
        List<HorarioPlanejamento> horariosPossiveis =
                gerarHorariosPossiveis(tamanhoGrade, aulas);
        restaurarPosicoesIniciais(aulas, horariosPersistidos, horariosPossiveis);
        if (!preservarEdicoesManuais) {
            aulas.forEach(aula -> aula.setBloqueadoManualmente(false));
        }

        List<GrupoSincronizacao> grupos =
                sincronizacaoService.identificarGruposSincronizacao(aulas);

        List<VinculoSincronizacao> vinculos =
                sincronizacaoService.identificarVinculosSincronizacao(grupos);

        GradePlanejamento grade = new GradePlanejamento(
                aulas,
                horariosPossiveis,
                professorRepository.findAllComDisciplinasPossiveis(),
                null,
                grupos,
                vinculos
        );
        grade.setTurmas(List.copyOf(turmas));
        return grade;
    }

    public record NovaTentativa(SolverJob<GradePlanejamento> job, SolverManager<GradePlanejamento> manager,
            long seed) implements AutoCloseable {
        @Override
        public void close() {
            manager.close();
        }
    }

    public NovaTentativa iniciarNovaTentativa(GradePlanejamento problema) {
        return iniciarNovaTentativa(problema, UUID.randomUUID());
    }

    public NovaTentativa iniciarNovaTentativa(GradePlanejamento problema, UUID problemId) {
        long seed = proximaSemente();
        SolverConfig config = new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withConstraintProviderClass(com.example.EduGrid.solver.constraint.GradeConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(spentLimit))
                .withRandomSeed(seed);
        SolverManager<GradePlanejamento> manager = SolverManager.create(
                ai.timefold.solver.core.api.solver.SolverFactory.create(config), new SolverManagerConfig());
        try {
            SolverJob<GradePlanejamento> job = manager.solve(problemId, problema);
            return new NovaTentativa(job, manager, seed);
        } catch (RuntimeException e) {
            manager.close();
            throw e;
        }
    }

    private long proximaSemente() {
        if (configuredSeed == null || configuredSeed.isBlank()) {
            return ThreadLocalRandom.current().nextLong();
        }
        try {
            return Long.parseLong(configuredSeed.trim()) + seedSequence.getAndIncrement();
        } catch (NumberFormatException e) {
            throw new IllegalStateException("edugrid.solver.random-seed deve ser um número inteiro.", e);
        }
    }

    public SolverJob<GradePlanejamento> iniciarOtimizacao(
            List<Turma> turmas,
            TamanhoGrade tamanhoGrade) {
        return iniciarOtimizacao(turmas, tamanhoGrade, null);
    }

    public SolverJob<GradePlanejamento> iniciarOtimizacao(
            List<Turma> turmas,
            TamanhoGrade tamanhoGrade,
            GradeHoraria gradeHoraria) {
        GradePlanejamento problema = montarProblema(turmas, tamanhoGrade);
        PreparacaoOtimizacao.preparar(problema);
        SolverJob<GradePlanejamento> solverJob = solverManager.solve(
                UUID.randomUUID(),
                problema
        );
        return new SolverJobComPersistencia(solverJob, persistenciaGradeService, gradeHoraria);
    }

    private void restaurarPosicoesIniciais(
            List<AulaPlanejamento> aulas,
            List<Horario> horariosPersistidos,
            List<HorarioPlanejamento> horariosPossiveis) {
        Map<ChaveBlocos, List<AulaPlanejamento>> aulasPorBloco = aulas.stream()
                .collect(Collectors.groupingBy(aula -> new ChaveBlocos(
                        aula.getTurma().getId(),
                        aula.getDisciplina().getId()
                )));
        Map<ChaveBlocos, List<Horario>> horariosPorBloco = new HashMap<>();
        for (Horario horario : horariosPersistidos) {
            if (horario == null || horario.getTurma() == null
                    || horario.getTurma().getId() == null
                    || horario.getDisciplina() == null
                    || horario.getDisciplina().getId() == null) {
                continue;
            }
            ChaveBlocos chave = new ChaveBlocos(
                    horario.getTurma().getId(),
                    horario.getDisciplina().getId()
            );
            horariosPorBloco.computeIfAbsent(chave, ignorado -> new ArrayList<>()).add(horario);
        }

        Map<ChaveHorario, HorarioPlanejamento> horariosPorPosicao = horariosPossiveis.stream()
                .collect(Collectors.toMap(
                        horario -> new ChaveHorario(
                                horario.getDiaSemana(),
                                horario.getTurno(),
                                horario.getPeriodoInicial()
                        ),
                        horario -> horario
                ));

        for (Map.Entry<ChaveBlocos, List<AulaPlanejamento>> entrada : aulasPorBloco.entrySet()) {
            List<BlocoPersistido> blocosPersistidos = identificarBlocosPersistidos(
                    horariosPorBloco.getOrDefault(entrada.getKey(), List.of())
            );
            if (blocosPersistidos.isEmpty()) {
                continue;
            }

            Map<Integer, List<AulaPlanejamento>> aulasPorDuracao = entrada.getValue().stream()
                    .collect(Collectors.groupingBy(AulaPlanejamento::getDuracaoPeriodos));
            Map<Integer, List<BlocoPersistido>> blocosPorDuracao = blocosPersistidos.stream()
                    .collect(Collectors.groupingBy(BlocoPersistido::duracao));

            for (Map.Entry<Integer, List<AulaPlanejamento>> blocos : aulasPorDuracao.entrySet()) {
                List<BlocoPersistido> correspondencias = blocosPorDuracao.get(blocos.getKey());
                if (correspondencias == null || correspondencias.size() != blocos.getValue().size()) {
                    continue;
                }
                List<AulaPlanejamento> aulasOrdenadas = blocos.getValue().stream()
                        .sorted(Comparator.comparing(AulaPlanejamento::getId)).toList();
                List<BlocoPersistido> blocosOrdenados = correspondencias.stream()
                        .sorted(Comparator.comparingInt(BlocoPersistido::diaSemana)
                                .thenComparingInt(BlocoPersistido::turno)
                                .thenComparingInt(BlocoPersistido::periodoInicial)).toList();
                for (int i = 0; i < aulasOrdenadas.size(); i++) {
                    AulaPlanejamento aula = aulasOrdenadas.get(i);
                    BlocoPersistido blocoPersistido = blocosOrdenados.get(i);
                    HorarioPlanejamento horarioInicial = horariosPorPosicao.get(new ChaveHorario(
                            blocoPersistido.diaSemana(), blocoPersistido.turno(), blocoPersistido.periodoInicial()));
                    if (horarioInicial != null) {
                        aula.setHorarioInicial(horarioInicial);
                        aula.setProfessor(blocoPersistido.professor());
                        aula.setBloqueadoManualmente(blocoPersistido.manualmenteEditado()
                                && blocoPersistido.professor() != null);
                    }
                }
            }
        }
    }

    private List<BlocoPersistido> identificarBlocosPersistidos(List<Horario> horarios) {
        Map<ChaveDiaTurno, List<Horario>> horariosPorDiaETurno = new HashMap<>();
        Map<ChaveDiaTurno, Set<Integer>> periodosUnicosPorDiaETurno = new HashMap<>();
        for (Horario horario : horarios) {
            if (horario.getPeriodoDia() < 0 || horario.getDiaSemana() < 0 || horario.getTurno() < 0) {
                return List.of();
            }
            ChaveDiaTurno chave = new ChaveDiaTurno(horario.getDiaSemana(), horario.getTurno());
            Set<Integer> periodosUnicos = periodosUnicosPorDiaETurno.computeIfAbsent(
                    chave,
                    ignorado -> new HashSet<>()
            );
            if (!periodosUnicos.add(horario.getPeriodoDia())) {
                return List.of();
            }
            horariosPorDiaETurno.computeIfAbsent(chave, ignorado -> new ArrayList<>()).add(horario);
        }

        List<BlocoPersistido> blocos = new ArrayList<>();
        for (Map.Entry<ChaveDiaTurno, List<Horario>> entrada : horariosPorDiaETurno.entrySet()) {
            List<Horario> periodos = entrada.getValue().stream()
                    .sorted(Comparator.comparingInt(Horario::getPeriodoDia)).toList();
            List<Horario> atual = new ArrayList<>();
            for (Horario horario : periodos) {
                if (!atual.isEmpty()) {
                    Horario anterior = atual.get(atual.size() - 1);
                    boolean separa = horario.getPeriodoDia() != anterior.getPeriodoDia() + 1
                            || !java.util.Objects.equals(idProfessor(horario), idProfessor(anterior))
                            || horario.isManualmenteEditado() != anterior.isManualmenteEditado();
                    if (separa) {
                        blocos.add(blocoPersistido(entrada.getKey(), atual));
                        atual = new ArrayList<>();
                    }
                }
                atual.add(horario);
            }
            if (!atual.isEmpty()) blocos.add(blocoPersistido(entrada.getKey(), atual));
        }
        return blocos.stream()
                .sorted(Comparator.comparingInt(BlocoPersistido::diaSemana)
                        .thenComparingInt(BlocoPersistido::turno)
                        .thenComparingInt(BlocoPersistido::periodoInicial))
                .toList();
    }

    private List<HorarioPlanejamento> gerarHorariosPossiveis(
            TamanhoGrade tamanhoGrade,
            List<AulaPlanejamento> aulas) {

        List<HorarioPlanejamento> horarios = new ArrayList<>();
        Set<Integer> turnosAtivos = new TreeSet<>();
        for (AulaPlanejamento aula : aulas) {
            if (aula.getTurma().getCurso() == null) {
                throw new IllegalArgumentException(
                        "A turma precisa ter um curso e turno definidos: " + aula.getTurma().getNome());
            }
            turnosAtivos.add(HorarioPlanejamento.indiceDoTurno(aula.getTurma().getCurso().getTurno()));
        }

        if (turnosAtivos.isEmpty()) {
            for (int turno = 0; turno < tamanhoGrade.getQtdeTurnos(); turno++) {
                turnosAtivos.add(turno);
            }
        } else if (turnosAtivos.size() > tamanhoGrade.getQtdeTurnos()) {
            throw new IllegalArgumentException(
                    "A quantidade de turnos das turmas excede a configuração da grade.");
        }

        int indiceTurnoDisponibilidade = 0;
        for (int turno : turnosAtivos) {
            int indiceDisponibilidade = tamanhoGrade.getQtdeTurnos() == 3
                    ? turno
                    : indiceTurnoDisponibilidade;

            for (int dia = 0; dia < tamanhoGrade.getQtdeDiasSemana(); dia++) {
                for (int periodo = 0; periodo < tamanhoGrade.getQtdePeriodosDia(); periodo++) {
                    horarios.add(new HorarioPlanejamento(
                            dia,
                            periodo,
                            turno,
                            indiceDisponibilidade
                    ));
                }
            }

            indiceTurnoDisponibilidade++;
        }

        return horarios;
    }

    private record ChaveBlocos(Integer turmaId, Integer disciplinaId) {
    }

    private record ChaveDiaTurno(int diaSemana, int turno) {
    }

    private record ChaveHorario(int diaSemana, int turno, int periodoInicial) {
    }

    private Integer idProfessor(Horario horario) {
        return horario.getProfessor() == null ? null : horario.getProfessor().getId();
    }

    private BlocoPersistido blocoPersistido(ChaveDiaTurno chave, List<Horario> linhas) {
        Horario primeiro = linhas.get(0);
        boolean editado = linhas.stream().allMatch(Horario::isManualmenteEditado);
        return new BlocoPersistido(chave.diaSemana(), chave.turno(), primeiro.getPeriodoDia(),
                linhas.size(), primeiro.getProfessor(), editado);
    }

    private record BlocoPersistido(int diaSemana, int turno, int periodoInicial, int duracao,
            com.example.EduGrid.model.Professor professor, boolean manualmenteEditado) {
    }

    private static final class SolverJobComPersistencia implements SolverJob<GradePlanejamento> {

        private final SolverJob<GradePlanejamento> solverJob;
        private final PersistenciaGradeService persistenciaGradeService;
        private final GradeHoraria gradeHoraria;
        private GradePlanejamento solucaoPersistida;

        private SolverJobComPersistencia(
                SolverJob<GradePlanejamento> solverJob,
                PersistenciaGradeService persistenciaGradeService,
                GradeHoraria gradeHoraria) {
            this.solverJob = solverJob;
            this.persistenciaGradeService = persistenciaGradeService;
            this.gradeHoraria = gradeHoraria;
        }

        @Override
        public Object getProblemId() {
            return solverJob.getProblemId();
        }

        @Override
        public SolverStatus getSolverStatus() {
            return solverJob.getSolverStatus();
        }

        @Override
        public CompletableFuture<Void> addProblemChanges(List<ProblemChange<GradePlanejamento>> problemChangeList) {
            return solverJob.addProblemChanges(problemChangeList);
        }

        @Override
        public void terminateEarly() {
            solverJob.terminateEarly();
        }

        @Override
        public boolean isTerminatedEarly() {
            return solverJob.isTerminatedEarly();
        }

        @Override
        public synchronized GradePlanejamento getFinalBestSolution()
                throws InterruptedException, ExecutionException {
            if (solucaoPersistida != null) {
                return solucaoPersistida;
            }
            GradePlanejamento solucao = solverJob.getFinalBestSolution();
            String relatorioRestricoesHard = "";
            if (solucao.getScore() != null && !solucao.getScore().isFeasible()) {
                relatorioRestricoesHard = DiagnosticoOtimizacao.descreverViolacoesHard(solucao);
                org.slf4j.LoggerFactory.getLogger(OtimizacaoService.class)
                        .warn("Busca terminou com violações hard; persistindo melhor solução candidata. {}",
                                relatorioRestricoesHard);
            }
            if (gradeHoraria == null) {
                persistenciaGradeService.persistirResultado(solucao);
            } else {
                persistenciaGradeService.persistirResultado(solucao, gradeHoraria, relatorioRestricoesHard);
            }
            solucaoPersistida = solucao;
            return solucao;
        }

        @Override
        public Duration getSolvingDuration() {
            return solverJob.getSolvingDuration();
        }

        @Override
        public long getScoreCalculationCount() {
            return solverJob.getScoreCalculationCount();
        }

        @Override
        public long getMoveEvaluationCount() {
            return solverJob.getMoveEvaluationCount();
        }

        @Override
        public ProblemSizeStatistics getProblemSizeStatistics() {
            return solverJob.getProblemSizeStatistics();
        }

        @Override
        public long getScoreCalculationSpeed() {
            return solverJob.getScoreCalculationSpeed();
        }

        @Override
        public long getMoveEvaluationSpeed() {
            return solverJob.getMoveEvaluationSpeed();
        }
    }
}
