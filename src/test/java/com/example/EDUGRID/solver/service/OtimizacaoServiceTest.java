package com.example.EduGrid.solver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.ProfessorRepository;
import com.example.EduGrid.service.BlocoDisciplinaService;
import com.example.EduGrid.solver.constraint.GradeConstraintProvider;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

class OtimizacaoServiceTest {

    @Test
    void montarProblemaCriaTodosOsBlocosEIdsUnicos() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Disciplina disciplina = criarDisciplina(1, 4.5, 2, 4, tamanhoGrade);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(disciplina),
                new Curso("Informática", "Técnico", "matutino"),
                tamanhoGrade
        );
        OtimizacaoService service = criarService(List.of(), List.of());

        GradePlanejamento problema = service.montarProblema(
                List.of(turma),
                tamanhoGrade
        );

        assertEquals(2, problema.getAulas().size());
        assertEquals(
                6,
                problema.getAulas().stream()
                        .mapToInt(AulaPlanejamento::getDuracaoPeriodos)
                        .sum()
        );

        long idsUnicos = problema.getAulas().stream()
                .map(AulaPlanejamento::getId)
                .distinct()
                .count();
        assertEquals(problema.getAulas().size(), idsUnicos);
    }

    @Test
    void associaSomenteBlocosPersistidosComDuracaoInequivoca() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Disciplina disciplina = criarDisciplina(1, 4.5, 2, 4, tamanhoGrade);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(disciplina),
                new Curso("Informática", "Técnico", "matutino"),
                tamanhoGrade
        );
        List<Horario> horarios = List.of(
                criarHorario(turma, disciplina, 0, 0),
                criarHorario(turma, disciplina, 0, 1),
                criarHorario(turma, disciplina, 0, 2),
                criarHorario(turma, disciplina, 0, 3),
                criarHorario(turma, disciplina, 1, 1),
                criarHorario(turma, disciplina, 1, 2)
        );
        OtimizacaoService service = criarService(horarios, List.of());

        GradePlanejamento problema = service.montarProblema(List.of(turma), tamanhoGrade);

        AulaPlanejamento blocoDeQuatro = problema.getAulas().stream()
                .filter(aula -> aula.getDuracaoPeriodos() == 4)
                .findFirst()
                .orElseThrow();
        AulaPlanejamento blocoDeDois = problema.getAulas().stream()
                .filter(aula -> aula.getDuracaoPeriodos() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals(0, blocoDeQuatro.getHorarioInicial().getDiaSemana());
        assertEquals(0, blocoDeQuatro.getHorarioInicial().getPeriodoInicial());
        assertEquals(1, blocoDeDois.getHorarioInicial().getDiaSemana());
        assertEquals(1, blocoDeDois.getHorarioInicial().getPeriodoInicial());
    }

    @Test
    void naoInfereBlocosQuandoPeriodosConsecutivosNaoDistinguemADivisao() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 6, 5);
        Disciplina disciplina = criarDisciplina(1, 4.5, 2, 4, tamanhoGrade);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(disciplina),
                new Curso("Informática", "Técnico", "matutino"),
                tamanhoGrade
        );
        List<Horario> horarios = List.of(
                criarHorario(turma, disciplina, 0, 0),
                criarHorario(turma, disciplina, 0, 1),
                criarHorario(turma, disciplina, 0, 2),
                criarHorario(turma, disciplina, 0, 3),
                criarHorario(turma, disciplina, 0, 4),
                criarHorario(turma, disciplina, 0, 5)
        );
        OtimizacaoService service = criarService(horarios, List.of());

        GradePlanejamento problema = service.montarProblema(List.of(turma), tamanhoGrade);

        assertEquals(2, problema.getAulas().size());
        assertNull(problema.getAulas().get(0).getHorarioInicial());
        assertNull(problema.getAulas().get(1).getHorarioInicial());
    }

    @Test
    void reconstruirVersaoUsaHorariosDaGradeSelecionadaSemRecalcularCargaEReconheceEdicoesFixadas() {
        TamanhoGrade tamanhoOrigemDisciplina = new TamanhoGrade(3, 5, 5);
        tamanhoOrigemDisciplina.setTamanhoPeriodo(45);
        TamanhoGrade tamanhoNovaConfiguracao = new TamanhoGrade(3, 5, 5);
        tamanhoNovaConfiguracao.setTamanhoPeriodo(60);
        Disciplina disciplina = criarDisciplina(11, 1.0, 1, 2, tamanhoOrigemDisciplina);
        Turma turma = new Turma(11, "INFO1", List.of(disciplina),
                new Curso("Informática", "Técnico", "matutino"), tamanhoNovaConfiguracao, 1);
        Professor professor = criarProfessor(disciplina, tamanhoNovaConfiguracao);
        GradeHoraria origem = new GradeHoraria(List.of(turma), tamanhoNovaConfiguracao);
        origem.setId(88);
        List<Horario> rows = List.of(
                criarHorario(turma, disciplina, 0, 1, professor, origem, true),
                criarHorario(turma, disciplina, 0, 2, professor, origem, true));
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        when(horarioRepository.findByGradeHoraria(origem)).thenReturn(rows);
        ProfessorRepository professorRepository = mock(ProfessorRepository.class);
        when(professorRepository.findAllComDisciplinasPossiveis()).thenReturn(List.of(professor));
        OtimizacaoService service = new OtimizacaoService(new BlocoDisciplinaService(),
                new IdentificacaoSincronizacaoService(), horarioRepository, professorRepository,
                mockSolverManager(), mock(PersistenciaGradeService.class));

        GradePlanejamento problema = service.montarProblema(
                List.of(turma), tamanhoNovaConfiguracao, origem, true);

        assertEquals(2, disciplina.getNumPeriodos(), "O tamanho de período da nova configuração não deve recalcular o cadastro.");
        assertEquals(2, problema.getAulas().get(0).getDuracaoPeriodos());
        assertEquals(1, problema.getAulas().get(0).getHorarioInicial().getPeriodoInicial());
        assertEquals(professor, problema.getAulas().get(0).getProfessor());
        assertTrue(problema.getAulas().get(0).isBloqueadoManualmente());
    }

    @Test
    void solverAtribuiProfessorEHorarioSemViolacoesHard() throws Exception {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Curso curso = new Curso("Informática", "Técnico", "matutino");
        Disciplina disciplina = criarDisciplina(1, 4.5, 2, 4, tamanhoGrade);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(disciplina),
                curso,
                tamanhoGrade,
                1
        );
        Professor professor = criarProfessor(disciplina, tamanhoGrade);
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withConstraintProviderClass(GradeConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig().withSpentLimit(Duration.ofSeconds(1))
                );

        try (SolverManager<GradePlanejamento> solverManager = SolverManager.create(
                SolverFactory.create(solverConfig),
                new SolverManagerConfig()
        )) {
            OtimizacaoService service = criarService(
                    List.of(),
                    List.of(professor),
                    solverManager
            );
            GradePlanejamento resultado = service.iniciarOtimizacao(
                    List.of(turma),
                    tamanhoGrade
            ).getFinalBestSolution();

            assertNotNull(resultado.getScore());
            assertEquals(0, resultado.getScore().hardScore());
            assertEquals(2, resultado.getAulas().size());
            assertEquals(
                    6,
                    resultado.getAulas().stream()
                            .mapToInt(AulaPlanejamento::getDuracaoPeriodos)
                            .sum()
            );
            for (AulaPlanejamento aula : resultado.getAulas()) {
                assertNotNull(aula.getProfessor());
                assertNotNull(aula.getHorarioInicial());
            }
        }
    }

    @Test
    void geraTurnoNoturnoMesmoQuandoAConfiguracaoContemUmTurno() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Disciplina disciplina = criarDisciplina(1, 0.75, 1, 1, tamanhoGrade);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(disciplina),
                new Curso("Informática", "Técnico", "noturno"),
                tamanhoGrade,
                1
        );
        OtimizacaoService service = criarService(List.of(), List.of());

        GradePlanejamento problema = service.montarProblema(List.of(turma), tamanhoGrade);

        assertEquals(
                java.util.Set.of(2),
                problema.getHorariosPossiveis().stream()
                        .map(HorarioPlanejamento::getTurno)
                        .collect(java.util.stream.Collectors.toSet())
        );
        assertEquals(
                java.util.Set.of(0),
                problema.getHorariosPossiveis().stream()
                        .map(HorarioPlanejamento::getIndiceTurnoDisponibilidade)
                        .collect(java.util.stream.Collectors.toSet())
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void falhaNaPersistenciaEhExpostaPeloResultadoDoJob() throws Exception {
        SolverManager<GradePlanejamento> solverManager = mockSolverManager();
        SolverJob<GradePlanejamento> solverJob = mock(SolverJob.class);
        GradePlanejamento solucao = mock(GradePlanejamento.class);
        when(solverManager.solve(any(), any())).thenReturn(solverJob);
        when(solverJob.getFinalBestSolution()).thenReturn(solucao);

        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        when(horarioRepository.findByTurmaInAndGradeHorariaIsNull(anyCollection())).thenReturn(List.of());
        ProfessorRepository professorRepository = mock(ProfessorRepository.class);
        TamanhoGrade tamanho = new TamanhoGrade(1, 5, 5);
        Disciplina disciplina = criarDisciplina(91, 1.5, 1, 2, tamanho);
        Turma turma = new Turma(91, "Teste", List.of(disciplina),
                new Curso("Teste", "Tecnico", "matutino"), tamanho);
        when(professorRepository.findAllComDisciplinasPossiveis())
                .thenReturn(List.of(criarProfessor(disciplina, tamanho)));
        PersistenciaGradeService persistenciaGradeService = mock(PersistenciaGradeService.class);
        doThrow(new IllegalStateException("Falha de gravação."))
                .when(persistenciaGradeService).persistirResultado(solucao);

        OtimizacaoService service = new OtimizacaoService(
                new BlocoDisciplinaService(),
                new IdentificacaoSincronizacaoService(),
                horarioRepository,
                professorRepository,
                solverManager,
                persistenciaGradeService
        );
        SolverJob<GradePlanejamento> job = service.iniciarOtimizacao(
                List.of(turma),
                tamanho
        );

        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                job::getFinalBestSolution
        );
        assertEquals("Falha de gravação.", erro.getMessage());
    }

    private OtimizacaoService criarService(List<Horario> horarios, List<Professor> professores) {
        return criarService(horarios, professores, mockSolverManager());
    }

    @SuppressWarnings("unchecked")
    private SolverManager<GradePlanejamento> mockSolverManager() {
        return mock(SolverManager.class);
    }

    private OtimizacaoService criarService(
            List<Horario> horarios,
            List<Professor> professores,
            SolverManager<GradePlanejamento> solverManager) {
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        when(horarioRepository.findByTurmaInAndGradeHorariaIsNull(anyCollection())).thenReturn(horarios);
        ProfessorRepository professorRepository = mock(ProfessorRepository.class);
        when(professorRepository.findAllComDisciplinasPossiveis()).thenReturn(professores);
        return new OtimizacaoService(
                new BlocoDisciplinaService(),
                new IdentificacaoSincronizacaoService(),
                horarioRepository,
                professorRepository,
                solverManager,
                mock(PersistenciaGradeService.class)
        );
    }

    private Disciplina criarDisciplina(
            int id,
            double cargaHoraria,
            int minimo,
            int maximo,
            TamanhoGrade tamanhoGrade) {
        Disciplina disciplina = new Disciplina(
                id,
                "Disciplina de teste",
                "Geral",
                cargaHoraria,
                tamanhoGrade,
                null,
                "comum"
        );
        disciplina.setMinPeriodosJuntos(minimo);
        disciplina.setMaxPeriodosJuntos(maximo);
        return disciplina;
    }

    private Horario criarHorario(Turma turma, Disciplina disciplina, int dia, int periodo) {
        return new Horario(null, 0, periodo, dia, turma, disciplina, null);
    }

    private Horario criarHorario(Turma turma, Disciplina disciplina, int dia, int periodo,
            Professor professor, GradeHoraria grade, boolean editado) {
        Horario horario = new Horario(null, 0, periodo, dia, turma, disciplina, professor);
        horario.setGradeHoraria(grade);
        horario.setManualmenteEditado(editado);
        return horario;
    }

    private Professor criarProfessor(Disciplina disciplina, TamanhoGrade tamanhoGrade) {
        Boolean[][][] disponibilidade = new Boolean[
                tamanhoGrade.getQtdeTurnos()
        ][
                tamanhoGrade.getQtdePeriodosDia()
        ][
                tamanhoGrade.getQtdeDiasSemana()
        ];
        for (Boolean[][] turno : disponibilidade) {
            for (Boolean[] periodos : turno) {
                Arrays.fill(periodos, Boolean.TRUE);
            }
        }
        Disciplina disciplinaHabilitada = new Disciplina();
        disciplinaHabilitada.setId(disciplina.getId());
        return new Professor(
                1,
                "Professor de teste",
                "Geral",
                1,
                List.of(),
                disponibilidade,
                List.of(disciplinaHabilitada)
        );
    }
}
