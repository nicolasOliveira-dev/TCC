package com.example.EduGrid.solver.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.service.OtimizacaoService.NovaTentativa;

import ai.timefold.solver.core.api.score.HardSoftScore;

/** Runs new-version attempts without replacing or persisting over the source grade. */
@Service
public class NovaVersaoGradeService {

    private final GradeHorariaRepository gradeRepository;
    private final HorarioRepository horarioRepository;
    private final OtimizacaoService otimizacaoService;
    private final PersistenciaGradeService persistenciaGradeService;
    private final Map<UUID, Tentativa> tentativas = new ConcurrentHashMap<>();

    public NovaVersaoGradeService(GradeHorariaRepository gradeRepository,
            HorarioRepository horarioRepository,
            OtimizacaoService otimizacaoService,
            PersistenciaGradeService persistenciaGradeService) {
        this.gradeRepository = gradeRepository;
        this.horarioRepository = horarioRepository;
        this.otimizacaoService = otimizacaoService;
        this.persistenciaGradeService = persistenciaGradeService;
    }

    @Transactional
    public UUID iniciar(Integer gradeId, boolean preservarEdicoesManuais) {
        UUID tentativaId = UUID.randomUUID();
        try {
            GradeHoraria origem = gradeRepository.findById(gradeId)
                    .orElseThrow(() -> new IllegalArgumentException("Grade de origem não encontrada."));
            List<Horario> horariosOrigem = horariosDaGrade(origem);
            if (horariosOrigem.isEmpty()) {
                throw new IllegalStateException("A grade selecionada não possui horários vinculados para servir como ponto de partida.");
            }

            GradePlanejamento problema = otimizacaoService.montarProblema(
                    origem.getTurmas(), origem.getTamanhoGrade(), origem, preservarEdicoesManuais);
            HardSoftScore scoreAtual = DiagnosticoOtimizacao.analisar(problema).pontuacao();
            Map<String, String> posicoesIniciais = problema.getAulas().stream().collect(
                    java.util.stream.Collectors.toMap(a -> a.getId(), a -> posicao(a)));
            PreparacaoOtimizacao.preparar(problema);
            NovaTentativa solver = otimizacaoService.iniciarNovaTentativa(problema, tentativaId);
            String snapshot = fingerprint(horariosOrigem);

            CompletableFuture<Resultado> futuro = CompletableFuture.supplyAsync(() -> {
                try (solver) {
                    GradePlanejamento solucao = solver.job().getFinalBestSolution();
                    DiagnosticoGrade diagnostico = DiagnosticoOtimizacao.analisar(solucao);
                    HardSoftScore scoreCandidato = diagnostico.pontuacao();
                    List<MudancaBloco> mudancas = solucao.getAulas().stream()
                            .filter(a -> !Objects.equals(posicoesIniciais.get(a.getId()), posicao(a)))
                            .map(a -> new MudancaBloco(a.getTurma().getNome(), a.getDisciplina().getNome(),
                                    a.getProfessor() == null ? "Sem professor" : a.getProfessor().getNome(),
                                    posicoesIniciais.getOrDefault(a.getId(), "Não reconstruída"), posicao(a),
                                    a.getDuracaoPeriodos()))
                            .toList();
                    return new Resultado(origem.getId(), scoreAtual, scoreCandidato,
                            DiagnosticoOtimizacao.descrever(solucao, diagnostico), diagnostico.viavel(),
                            scoreCandidato.compareTo(scoreAtual) > 0, solver.seed(), mudancas, solucao);
                } catch (Exception e) {
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            });
            tentativas.put(tentativaId, new Tentativa(origem.getId(), snapshot, futuro));
        } catch (Exception e) {
            tentativas.put(tentativaId, new Tentativa(gradeId, "", CompletableFuture.failedFuture(e)));
        }
        return tentativaId;
    }

    public String status(UUID tentativaId) {
        Tentativa tentativa = tentativas.get(tentativaId);
        if (tentativa == null) return "NAO_ENCONTRADO";
        if (!tentativa.futuro().isDone()) return "PROCESSANDO";
        return tentativa.futuro().isCompletedExceptionally() ? "FALHOU" : "CONCLUIDO";
    }

    public Resumo resumo(UUID tentativaId) {
        Tentativa tentativa = tentativas.get(tentativaId);
        if (tentativa == null) return null;
        if (tentativa.futuro().isCompletedExceptionally()) {
            return new Resumo(tentativa.gradeOrigemId(), null, null, false, false,
                    null, null, erro(tentativa.futuro()), null, null);
        }
        if (!tentativa.futuro().isDone()) {
            return new Resumo(tentativa.gradeOrigemId(), null, null, false, false,
                    null, null, "O Timefold está buscando uma nova distribuição.", null, null);
        }
        Resultado resultado = tentativa.futuro().join();
        return new Resumo(resultado.gradeOrigemId(), resultado.scoreAtual().toString(),
                resultado.scoreCandidato().toString(), resultado.viavel(), resultado.melhorou(),
                resultado.seed(), resultado.diagnostico(), null, resultado.mudancas(),
                resultado.viavel() && resultado.melhorou());
    }

    @Transactional
    public Integer aplicar(UUID tentativaId) {
        Tentativa tentativa = tentativas.get(tentativaId);
        if (tentativa == null || !tentativa.futuro().isDone() || tentativa.futuro().isCompletedExceptionally()) {
            throw new IllegalStateException("A candidata ainda não está disponível para aplicação.");
        }
        Resultado resultado = tentativa.futuro().join();
        if (!resultado.viavel() || !resultado.melhorou()) {
            throw new IllegalStateException("A candidata só pode ser aplicada quando for válida e tiver pontuação melhor que a grade atual.");
        }
        GradeHoraria origem = gradeRepository.findById(resultado.gradeOrigemId())
                .orElseThrow(() -> new IllegalStateException("A grade de origem não foi encontrada."));
        if (!fingerprint(horariosDaGrade(origem)).equals(tentativa.snapshot())) {
            tentativas.remove(tentativaId);
            throw new IllegalStateException("A grade original foi alterada enquanto a busca ocorria. Inicie uma nova tentativa para evitar sobrescrever essas alterações.");
        }
        // Each GradeHoraria must own its own collection wrapper. Reusing the
        // Hibernate PersistentCollection from the source grade makes Hibernate
        // detect the same collection as reachable from two entity owners.
        GradeHoraria novaVersao = new GradeHoraria(
                new java.util.ArrayList<>(origem.getTurmas()), origem.getTamanhoGrade());
        GradeHoraria salva = persistenciaGradeService.persistirResultado(resultado.solucao(), novaVersao);
        tentativas.remove(tentativaId);
        return salva.getId();
    }

    public void descartar(UUID tentativaId) {
        tentativas.remove(tentativaId);
    }

    private String fingerprint(List<Horario> horarios) {
        return horarios.stream().map(h -> String.join(":",
                String.valueOf(h.getId()), String.valueOf(h.getTurma() == null ? null : h.getTurma().getId()),
                String.valueOf(h.getDisciplina() == null ? null : h.getDisciplina().getId()),
                String.valueOf(h.getProfessor() == null ? null : h.getProfessor().getId()),
                String.valueOf(h.getDiaSemana()), String.valueOf(h.getTurno()), String.valueOf(h.getPeriodoDia()),
                String.valueOf(h.isManualmenteEditado())))
                .sorted().reduce((a, b) -> a + "|" + b).orElse("");
    }

    private List<Horario> horariosDaGrade(GradeHoraria grade) {
        List<Horario> horarios = horarioRepository.findByGradeHoraria(grade);
        return horarios.isEmpty()
                ? horarioRepository.findByTurmaInAndGradeHorariaIsNull(grade.getTurmas())
                : horarios;
    }

    private static String posicao(com.example.EduGrid.solver.domain.AulaPlanejamento aula) {
        var h = aula.getHorarioInicial();
        if (h == null) return "Sem posição";
        String[] dias = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
        String[] turnos = {"manhã", "tarde", "noite"};
        String dia = h.getDiaSemana() >= 0 && h.getDiaSemana() < dias.length ? dias[h.getDiaSemana()] : "dia " + (h.getDiaSemana() + 1);
        String turno = h.getTurno() >= 0 && h.getTurno() < turnos.length ? turnos[h.getTurno()] : "turno " + h.getTurno();
        return dia + " / " + turno + " / período " + (h.getPeriodoInicial() + 1);
    }

    private String erro(CompletableFuture<Resultado> futuro) {
        try { futuro.join(); return "Não foi possível concluir a tentativa."; }
        catch (CompletionException e) {
            Throwable causa = e;
            while (causa.getCause() != null) causa = causa.getCause();
            return causa.getMessage() == null ? "Não foi possível concluir a tentativa." : causa.getMessage();
        }
    }

    private record Tentativa(Integer gradeOrigemId, String snapshot, CompletableFuture<Resultado> futuro) {}
    private record Resultado(Integer gradeOrigemId, HardSoftScore scoreAtual,
            HardSoftScore scoreCandidato, String diagnostico, boolean viavel, boolean melhorou,
            long seed, List<MudancaBloco> mudancas, GradePlanejamento solucao) {}
    public record MudancaBloco(String turma, String disciplina, String professor,
            String horarioAnterior, String horarioCandidato, int duracaoPeriodos) {}
    public record Resumo(Integer gradeOrigemId, String pontuacaoAtual, String pontuacaoCandidata,
            boolean viavel, boolean melhorou, Long semente, String diagnostico, String erro,
            List<MudancaBloco> mudancas, Boolean podeAplicar) {}
}
