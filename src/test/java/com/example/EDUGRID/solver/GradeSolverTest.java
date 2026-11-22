package com.example.EduGrid.solver;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.solver.constraint.GradeConstraintProvider;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.GrupoSincronizacao;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;
import com.example.EduGrid.solver.domain.VinculoSincronizacao;
import com.example.EduGrid.solver.service.IdentificacaoSincronizacaoService;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

class GradeSolverTest {

    private static final int DIAS_LETIVOS = 5;
    private static final int PERIODOS_POR_DIA = 5;

    @Test
    void deveGerarGradeSemConflitos() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(2, PERIODOS_POR_DIA, DIAS_LETIVOS);
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        Disciplina disciplina = criarDisciplina(1, "Matematica", 2, tamanhoGrade);
        disciplina.setCurso(curso);
        Professor professor = criarProfessor(
                "Carlos", disponibilidade(DIAS_LETIVOS, DIAS_LETIVOS), disciplina);
        Turma turma = criarTurma(
                1, "Turma A", "matutino", List.of(disciplina), tamanhoGrade);

        AulaPlanejamento aula1 = new AulaPlanejamento();
        aula1.setId("A1");
        aula1.setTurma(turma);
        aula1.setDisciplina(disciplina);
        aula1.setDuracaoPeriodos(2);

        AulaPlanejamento aula2 = new AulaPlanejamento();
        aula2.setId("A2");
        aula2.setTurma(turma);
        aula2.setDisciplina(disciplina);
        aula2.setDuracaoPeriodos(2);
        aula1.setProfessor(professor);
        aula2.setProfessor(professor);

        List<HorarioPlanejamento> horarios = List.of(
                new HorarioPlanejamento(0, 0, 0),
                new HorarioPlanejamento(0, 2, 0)
        );

        GradePlanejamento problema = new GradePlanejamento(
                List.of(aula1, aula2),
                horarios,
                List.of(professor),
                null
        );

        SolverConfig config = new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withConstraintProviderClass(
                        GradeConstraintProvider.class
                )
                .withTerminationConfig(
                        new TerminationConfig()
                                .withSpentLimit(Duration.ofSeconds(5))
                );

        SolverFactory<GradePlanejamento> factory =
                SolverFactory.create(config);

        Solver<GradePlanejamento> solver =
                factory.buildSolver();

        GradePlanejamento resultado = solver.solve(problema);

        assertNotNull(resultado.getScore());
        assertEquals(0, resultado.getScore().hardScore(),
                "A grade otimizada não deve conter conflitos obrigatórios.");

        AulaPlanejamento aulaResultado1 = resultado.getAulas().get(0);
        AulaPlanejamento aulaResultado2 = resultado.getAulas().get(1);
        assertNotNull(aulaResultado1.getProfessor());
        assertNotNull(aulaResultado1.getHorarioInicial());
        assertNotNull(aulaResultado2.getProfessor());
        assertNotNull(aulaResultado2.getHorarioInicial());
        assertNotEquals(
                aulaResultado1.getHorarioInicial().getPeriodoInicial(),
                aulaResultado2.getHorarioInicial().getPeriodoInicial(),
                "As aulas da mesma turma devem ocupar horários diferentes."
        );
    }

    @Test
    void deveAplicarLimiteDeJornadaMaximaComoEsperado() throws Exception {
        GradeConstraintProvider provider = new GradeConstraintProvider();
        Method excedeJornadaMaxima = GradeConstraintProvider.class
                .getDeclaredMethod("excedeJornadaMaxima", List.class, int.class);
        excedeJornadaMaxima.setAccessible(true);

        assertFalse((Boolean) excedeJornadaMaxima.invoke(provider, List.of(
                aulaComPeriodo(0, 0, 0, 8)
        ), 8), "Oito períodos consecutivos devem ser aceitos.");

        assertTrue((Boolean) excedeJornadaMaxima.invoke(provider, List.of(
                aulaComPeriodo(0, 0, 0, 9)
        ), 8), "Nove períodos consecutivos devem violar a jornada máxima.");

        assertFalse((Boolean) excedeJornadaMaxima.invoke(provider, List.of(
                aulaComPeriodo(0, 0, 0, 5),
                aulaComPeriodo(0, 0, 6, 5)
        ), 8), "Um intervalo entre blocos deve preservar a jornada máxima.");

        assertFalse((Boolean) excedeJornadaMaxima.invoke(provider, List.of(
                aulaComPeriodo(0, 0, 0, 5),
                aulaComPeriodo(1, 0, 0, 4)
        ), 8), "Períodos em dias diferentes não devem ser somados na mesma jornada.");

        assertTrue((Boolean) excedeJornadaMaxima.invoke(provider, List.of(
                aulaComPeriodo(0, 0, 0, 4),
                aulaComPeriodo(0, 0, 4, 5)
        ), 8), "Blocos consecutivos de turmas distintas ainda devem ser contados juntos.");
    }

    @Test
    void deveAplicarRestricaoDeDescansoEntreTurnos() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(2, PERIODOS_POR_DIA, 7);
        Disciplina disciplina = criarDisciplina(1, "Matematica", 2, tamanhoGrade);

        Professor professor = criarProfessor("Carlos", disponibilidade(7, 7, 7), disciplina);
        Professor professorDiferente = criarProfessor("Pedro", disponibilidade(7, 7, 7), disciplina);

        Turma turmaManha = criarTurma(1, "Turma Manha", "matutino", List.of(disciplina), tamanhoGrade);
        Turma turmaTarde = criarTurma(2, "Turma Tarde", "vespertino", List.of(disciplina), tamanhoGrade);
        Turma turmaNoite = criarTurma(3, "Turma Noite", "noturno", List.of(disciplina), tamanhoGrade);

        assertEquals(-1, resolverHardScore(List.of(
                aulaComHorario(turmaNoite, disciplina, professor, 0, 2, 0),
                aulaComHorario(turmaManha, disciplina, professor, 1, 0, 0)
        ), professor).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaNoite, disciplina, professor, 0, 2, 0),
                aulaComHorario(turmaTarde, disciplina, professor, 1, 1, 0)
        ), professor).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaTarde, disciplina, professor, 0, 1, 0),
                aulaComHorario(turmaManha, disciplina, professor, 1, 0, 0)
        ), professor).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaNoite, disciplina, professor, 0, 2, 0),
                aulaComHorario(turmaManha, disciplina, professor, 2, 0, 0)
        ), professor).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaNoite, disciplina, professor, 0, 2, 0),
                aulaComHorario(turmaManha, disciplina, professorDiferente, 1, 0, 0)
        ), professor, professorDiferente).getScore().hardScore());

        assertEquals(-1, resolverHardScore(List.of(
                aulaComHorario(turmaNoite, disciplina, professor, 5, 2, 0),
                aulaComHorario(turmaManha, disciplina, professor, 6, 0, 0)
        ), professor).getScore().hardScore());
    }

    @Test
    void devePenalizarDisciplinaConsecutivaDiferenteDoMesmoProfessor() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(2, PERIODOS_POR_DIA, DIAS_LETIVOS);
        Disciplina matematica = criarDisciplina(1, "Matematica", 2, tamanhoGrade);
        Disciplina fisica = criarDisciplina(2, "Fisica", 2, tamanhoGrade);
        Professor professor = criarProfessor("Carlos", disponibilidade(DIAS_LETIVOS, DIAS_LETIVOS, DIAS_LETIVOS), matematica, fisica);
        Turma turma = criarTurma(1, "Turma A", "matutino", List.of(matematica, fisica), tamanhoGrade);

        assertEquals(-1, resolverHardScore(List.of(
                aulaComHorario(turma, matematica, professor, 0, 0, 0, 2),
                aulaComHorario(turma, fisica, professor, 0, 0, 2, 2)
        ), professor).getScore().softScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turma, matematica, professor, 0, 0, 0, 2),
                aulaComHorario(turma, fisica, professor, 0, 0, 3, 2)
        ), professor).getScore().softScore());

        Professor professor2 = criarProfessor("Pedro", disponibilidade(DIAS_LETIVOS, DIAS_LETIVOS, DIAS_LETIVOS), matematica, fisica);
        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turma, matematica, professor, 0, 0, 0, 2),
                aulaComHorario(turma, fisica, professor2, 0, 0, 2, 2)
        ), professor, professor2).getScore().softScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turma, matematica, professor, 0, 0, 0, 2),
                aulaComHorario(turma, matematica, professor, 0, 0, 2, 2)
        ), professor).getScore().softScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turma, matematica, professor, 0, 0, 0, 2),
                aulaComHorario(turma, fisica, professor, 1, 0, 0, 2)
        ), professor).getScore().softScore());
    }

    @Test
    void devePenalizarMesmaDisciplinaTecnicaEmHorarioSobrepostoMesmoCursoAno() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(2, PERIODOS_POR_DIA, DIAS_LETIVOS);
        Disciplina disciplinaTecnica = criarDisciplina(1, "Eletronica", 2, tamanhoGrade);
        disciplinaTecnica.setTipoDisciplina("tecnica");
        Disciplina disciplinaTecnicaOutra = criarDisciplina(2, "Robotica", 2, tamanhoGrade);
        disciplinaTecnicaOutra.setTipoDisciplina("tecnica");
        Disciplina disciplinaComum = criarDisciplina(3, "Historia", 2, tamanhoGrade);
        disciplinaComum.setTipoDisciplina("comum");

        Professor professor = criarProfessor("Carlos", disponibilidade(DIAS_LETIVOS, DIAS_LETIVOS, DIAS_LETIVOS), disciplinaTecnica, disciplinaTecnicaOutra, disciplinaComum);
        Professor professor2 = criarProfessor("Pedro", disponibilidade(DIAS_LETIVOS, DIAS_LETIVOS, DIAS_LETIVOS), disciplinaTecnica, disciplinaTecnicaOutra, disciplinaComum);
        Curso curso = new Curso("Tecnologia", "Técnico", "matutino");
        curso.setId(1);

        Turma turmaAno1A = criarTurma(1, "Turma A", "matutino", List.of(disciplinaTecnica), tamanhoGrade);
        turmaAno1A.setAno(2024);
        turmaAno1A.setCurso(curso);

        Turma turmaAno1B = criarTurma(2, "Turma B", "matutino", List.of(disciplinaTecnica), tamanhoGrade);
        turmaAno1B.setAno(2024);
        turmaAno1B.setCurso(curso);

        Turma turmaAno2 = criarTurma(3, "Turma C", "matutino", List.of(disciplinaTecnica), tamanhoGrade);
        turmaAno2.setAno(2025);
        turmaAno2.setCurso(curso);

        Turma turmaCursoDiferente = criarTurma(4, "Turma D", "matutino", List.of(disciplinaTecnica), tamanhoGrade);
        turmaCursoDiferente.setAno(2024);
        Curso cursoDiferente = new Curso("Administração", "Técnico", "matutino");
        cursoDiferente.setId(2);
        turmaCursoDiferente.setCurso(cursoDiferente);

        Turma turmaComum = criarTurma(5, "Turma E", "matutino", List.of(disciplinaComum), tamanhoGrade);
        turmaComum.setAno(2024);
        turmaComum.setCurso(curso);

        assertEquals(-1, resolverHardScore(List.of(
                aulaComHorario(turmaAno1A, disciplinaTecnica, professor, 0, 0, 0, 2),
                aulaComHorario(turmaAno1B, disciplinaTecnica, professor2, 0, 0, 1, 2)
        ), professor, professor2).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaAno1A, disciplinaTecnica, professor, 0, 0, 0, 2),
                aulaComHorario(turmaAno2, disciplinaTecnica, professor2, 0, 0, 1, 2)
        ), professor, professor2).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaAno1A, disciplinaTecnica, professor, 0, 0, 0, 2),
                aulaComHorario(turmaComum, disciplinaComum, professor2, 0, 0, 1, 2)
        ), professor, professor2).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaAno1A, disciplinaTecnica, professor, 0, 0, 0, 2),
                aulaComHorario(turmaCursoDiferente, disciplinaTecnica, professor2, 0, 0, 1, 2)
        ), professor, professor2).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaAno1A, disciplinaComum, professor, 0, 0, 0, 2),
                aulaComHorario(turmaAno1B, disciplinaComum, professor2, 0, 0, 1, 2)
        ), professor, professor2).getScore().hardScore());

        assertEquals(0, resolverHardScore(List.of(
                aulaComHorario(turmaAno1A, disciplinaTecnica, professor, 0, 0, 0, 2),
                aulaComHorario(turmaAno1B, disciplinaTecnica, professor2, 0, 0, 3, 2)
        ), professor, professor2).getScore().hardScore());
    }

    @Test
    void deveEncontrarSolucaoComTodasAsRestricoesParaDuasTurmas() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(2, PERIODOS_POR_DIA, DIAS_LETIVOS);
        List<Disciplina> disciplinas = List.of(
                criarDisciplina(1, "Matematica", 2, tamanhoGrade),
                criarDisciplina(2, "Portugues", 3, tamanhoGrade),
                criarDisciplina(3, "Ciencias", 2, tamanhoGrade),
                criarDisciplina(4, "Historia", 3, tamanhoGrade));

        Turma matutina = criarTurma(1, "Turma Matutina", "matutino", disciplinas, tamanhoGrade);
        Turma vespertina = criarTurma(2, "Turma Vespertina", "vespertino", disciplinas, tamanhoGrade);

        Professor professor1 = criarProfessor(
                "Ana", disponibilidade(5, 3), disciplinas.get(0), disciplinas.get(1));
        Professor professor2 = criarProfessor(
                "Bruno", disponibilidade(4, 5), disciplinas.get(1), disciplinas.get(2));
        Professor professor3 = criarProfessor(
                "Carla", disponibilidade(4, 4), disciplinas.get(2), disciplinas.get(3));

        List<HorarioPlanejamento> horarios = new ArrayList<>();
        for (int turno = 0; turno < 2; turno++) {
            for (int dia = 0; dia < DIAS_LETIVOS; dia++) {
                for (int periodo = 0; periodo < PERIODOS_POR_DIA; periodo++) {
                    horarios.add(new HorarioPlanejamento(dia, periodo, turno));
                }
            }
        }

        List<AulaPlanejamento> aulas = new ArrayList<>();
        int id = 1;
        for (Turma turma : List.of(matutina, vespertina)) {
            for (Disciplina disciplina : disciplinas) {
                aulas.add(new AulaPlanejamento(
                        "Aula-" + id++,
                        turma,
                        disciplina,
                        disciplina.getNumPeriodos(),
                        null,
                        null));
            }
        }

        GradePlanejamento problema = new GradePlanejamento(
                aulas,
                horarios,
                List.of(professor1, professor2, professor3),
                null);

        SolverConfig config = new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withConstraintProviderClass(GradeConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig().withSpentLimit(Duration.ofSeconds(1)));

        GradePlanejamento resultado = SolverFactory.<GradePlanejamento>create(config)
                .buildSolver()
                .solve(problema);

        assertNotNull(resultado.getScore());
        assertEquals(8, resultado.getAulas().size());

        for (AulaPlanejamento aula : resultado.getAulas()) {
            assertNotNull(aula.getProfessor(), "Cada bloco deve receber um professor.");
            assertNotNull(aula.getHorarioInicial(), "Cada bloco deve receber um horário.");

            HorarioPlanejamento horario = aula.getHorarioInicial();
            int turnoEsperado = "matutino".equals(aula.getTurma().getCurso().getTurno()) ? 0 : 1;

            assertEquals(turnoEsperado, horario.getTurno());
            assertTrue(horario.getDiaSemana() >= 0 && horario.getDiaSemana() < DIAS_LETIVOS);
            assertTrue(horario.getPeriodoInicial() >= 0);
            assertTrue(horario.getPeriodoInicial() + aula.getDuracaoPeriodos()
                    <= PERIODOS_POR_DIA,
                    () -> aula.getId() + " exceeds daily periods: start="
                            + horario.getPeriodoInicial() + ", duration="
                            + aula.getDuracaoPeriodos());
            assertTrue(aula.getProfessor().getDisciplinasPossiveis()
                    .contains(aula.getDisciplina()));
            assertTrue(Boolean.TRUE.equals(aula.getProfessor().getDisponibilidadeHoraria()
                    [horario.getTurno()][horario.getPeriodoInicial()][horario.getDiaSemana()]),
                    () -> aula.getId() + " assigned to unavailable slot: professor="
                            + aula.getProfessor().getNome() + ", turn=" + horario.getTurno()
                            + ", day=" + horario.getDiaSemana() + ", period="
                            + horario.getPeriodoInicial());
        }

        assertEquals(0, resultado.getScore().hardScore(),
                "Todas as restrições obrigatórias devem ser satisfeitas.");
    }

    @Test
    void deveOtimizarGradeDeTresAnosComGruposEVinculosGerados() {
        CenarioSincronizacao cenario = criarCenarioSincronizacao(false);
        GradePlanejamento problema = cenario.problema();

        assertEquals(1, problema.getGruposSincronizacao().size());
        assertEquals(3, problema.getVinculosSincronizacao().size());
        assertTrue(problema.getGruposSincronizacao().get(0).blocosPorTurma().containsKey(101));
        assertTrue(problema.getGruposSincronizacao().get(0).blocosPorTurma().containsKey(102));

        Solver<GradePlanejamento> solver = SolverFactory.<GradePlanejamento>create(
                solverConfig(EnvironmentMode.FULL_ASSERT))
                .buildSolver();
        AtomicReference<HardSoftScore> scoreInicial = new AtomicReference<>();
        solver.addEventListener(event -> {
            if (event.isNewBestSolutionInitialized()) {
                scoreInicial.compareAndSet(null, (HardSoftScore) event.getNewBestScore());
            }
        });
        GradePlanejamento resultado = solver.solve(problema);

        assertNotNull(resultado.getScore());
        assertNotNull(scoreInicial.get(), "O solver deve publicar o score inicial da solução viável.");
        assertEquals(0, scoreInicial.get().hardScore(),
                "A grade inicial deve ser viável antes da otimização.");
        assertEquals(cenario.scoreSoftInicial(), scoreInicial.get().softScore());
        assertEquals(0, resultado.getScore().hardScore(),
                "A solução deve satisfazer todas as restrições hard.");
        assertTrue(resultado.getScore().softScore() >= scoreInicial.get().softScore(),
                "O solver não deve retornar score soft inferior ao da grade inicial.");
    }

    @Test
    void deveManterScoreIncrementalConsistenteComGrupoDeTresTurmas() {
        CenarioSincronizacao cenario = criarCenarioSincronizacao(true);
        GradePlanejamento problema = cenario.problema();

        assertEquals(1, problema.getGruposSincronizacao().size());
        assertEquals(5, problema.getVinculosSincronizacao().size());

        Solver<GradePlanejamento> solver = SolverFactory.<GradePlanejamento>create(
                solverConfig(EnvironmentMode.FULL_ASSERT))
                .buildSolver();
        AtomicReference<HardSoftScore> scoreInicial = new AtomicReference<>();
        solver.addEventListener(event -> {
            if (event.isNewBestSolutionInitialized()) {
                scoreInicial.compareAndSet(null, (HardSoftScore) event.getNewBestScore());
            }
        });
        GradePlanejamento resultado = solver.solve(problema);

        assertNotNull(resultado.getScore());
        assertNotNull(scoreInicial.get(), "O solver deve publicar o score inicial da solução viável.");
        assertEquals(0, scoreInicial.get().hardScore(),
                "A grade inicial deve ser viável antes da otimização.");
        assertEquals(cenario.scoreSoftInicial(), scoreInicial.get().softScore());
        assertEquals(0, resultado.getScore().hardScore(),
                "O grupo de três turmas deve permanecer viável.");
        assertTrue(resultado.getScore().softScore() >= scoreInicial.get().softScore(),
                "A solução final deve preservar ou melhorar o score soft inicial.");
    }

    private SolverConfig solverConfig(EnvironmentMode environmentMode) {
        return new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withConstraintProviderClass(GradeConstraintProvider.class)
                .withEnvironmentMode(environmentMode)
                .withRandomSeed(1L)
                .withTerminationConfig(
                        new TerminationConfig().withSpentLimit(Duration.ofSeconds(1)));
    }

    private CenarioSincronizacao criarCenarioSincronizacao(boolean grupoDeTresTurmas) {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, PERIODOS_POR_DIA, DIAS_LETIVOS);
        Curso curso = new Curso("Informatica", "Tecnico", "matutino");
        curso.setId(100);

        Disciplina algoritmos = disciplinaTecnica(1, "Algoritmos", 4, tamanhoGrade, curso);
        Disciplina lpI = disciplinaTecnica(2, "LP I", 2, tamanhoGrade, curso);
        Disciplina bdI = disciplinaTecnica(3, "BD I", 2, tamanhoGrade, curso);
        Disciplina lpII = disciplinaTecnica(4, "LP II", 2, tamanhoGrade, curso);
        Disciplina bdII = disciplinaTecnica(5, "BD II", 2, tamanhoGrade, curso);

        Turma primeiroAno = new Turma(
                101, "INFO1", List.of(algoritmos), curso, tamanhoGrade, 1);
        Turma segundoAno = new Turma(
                102, "INFO2", List.of(lpI, bdI), curso, tamanhoGrade, 2);
        Turma terceiroAno = new Turma(
                103, "INFO3", List.of(lpII, bdII), curso, tamanhoGrade, 3);

        Professor professorAlgoritmos = professorTecnico("Prof. Algoritmos", algoritmos);
        Professor professorLpI = professorTecnico("Prof. LP I", lpI);
        Professor professorBdI = professorTecnico("Prof. BD I", bdI);
        Professor professorLpII = professorTecnico("Prof. LP II", lpII);
        Professor professorBdII = professorTecnico("Prof. BD II", bdII);

        List<HorarioPlanejamento> horarios = new ArrayList<>();
        for (int dia = 0; dia < DIAS_LETIVOS; dia++) {
            for (int periodo = 0; periodo <= 3; periodo++) {
                horarios.add(new HorarioPlanejamento(dia, periodo, 0));
            }
        }

        AulaPlanejamento aulaAlgoritmos = aulaPlanejada(
                "algoritmos-1", primeiroAno, algoritmos, professorAlgoritmos,
                horarioInicial(horarios, 0), 4);
        AulaPlanejamento aulaLpI = aulaPlanejada(
                "lp-i-2", segundoAno, lpI, professorLpI, horarioInicial(horarios, 0), 2);
        AulaPlanejamento aulaBdI = aulaPlanejada(
                "bd-i-2", segundoAno, bdI, professorBdI, horarioInicial(horarios, 3), 2);
        AulaPlanejamento aulaLpII = aulaPlanejada(
                "lp-ii-3", terceiroAno, lpII, professorLpII,
                horarioInicial(horarios, grupoDeTresTurmas ? 1 : 0), 2);
        AulaPlanejamento aulaBdII = aulaPlanejada(
                "bd-ii-3", terceiroAno, bdII, professorBdII,
                horarioInicial(horarios, grupoDeTresTurmas ? 3 : 2), 2);

        List<AulaPlanejamento> aulas = List.of(
                aulaAlgoritmos, aulaLpI, aulaBdI, aulaLpII, aulaBdII);
        IdentificacaoSincronizacaoService identificacao
                = new IdentificacaoSincronizacaoService();
        List<GrupoSincronizacao> grupos;
        int scoreSoftInicial;
        if (grupoDeTresTurmas) {
            grupos = List.of(new GrupoSincronizacao(
                    "grupo-tres-anos",
                    4,
                    Map.of(
                            primeiroAno.getId(), List.of(aulaAlgoritmos),
                            segundoAno.getId(), List.of(aulaLpI, aulaBdI),
                            terceiroAno.getId(), List.of(aulaLpII, aulaBdII)
                    )
            ));
            scoreSoftInicial = -20;
        } else {
            grupos = identificacao.identificarGruposSincronizacao(aulas);
            scoreSoftInicial = -10;
        }
        List<VinculoSincronizacao> vinculos
                = identificacao.identificarVinculosSincronizacao(grupos);

        GradePlanejamento problema = new GradePlanejamento(
                aulas,
                horarios,
                List.of(
                        professorAlgoritmos,
                        professorLpI,
                        professorBdI,
                        professorLpII,
                        professorBdII
                ),
                null,
                grupos,
                vinculos
        );
        return new CenarioSincronizacao(problema, scoreSoftInicial);
    }

    private Disciplina disciplinaTecnica(
            int id, String nome, int periodos, TamanhoGrade tamanhoGrade, Curso curso) {
        Disciplina disciplina = new Disciplina(
                id,
                nome,
                "Informatica",
                periodos * tamanhoGrade.getTamanhoPeriodo() / 60.0,
                tamanhoGrade,
                curso,
                "tecnica");
        return disciplina;
    }

    private Professor professorTecnico(String nome, Disciplina disciplina) {
        Boolean[][][] disponibilidade = new Boolean[3][PERIODOS_POR_DIA][DIAS_LETIVOS];
        for (Boolean[][] turno : disponibilidade) {
            for (Boolean[] periodo : turno) {
                Arrays.fill(periodo, true);
            }
        }
        return criarProfessor(nome, disponibilidade, disciplina);
    }

    private AulaPlanejamento aulaPlanejada(
            String id,
            Turma turma,
            Disciplina disciplina,
            Professor professor,
            HorarioPlanejamento horarioInicial,
            int duracao) {
        return new AulaPlanejamento(
                id,
                turma,
                disciplina,
                duracao,
                professor,
                horarioInicial
        );
    }

    private HorarioPlanejamento horarioInicial(
            List<HorarioPlanejamento> horarios, int periodoInicial) {
        return horarios.stream()
                .filter(horario -> horario.getDiaSemana() == 0
                        && horario.getPeriodoInicial() == periodoInicial
                        && horario.getTurno() == 0)
                .findFirst()
                .orElseThrow();
    }

    private record CenarioSincronizacao(
            GradePlanejamento problema,
            int scoreSoftInicial
    ) {
    }

    private AulaPlanejamento aulaComPeriodo(int diaSemana, int turno, int periodoInicial, int duracaoPeriodos) {
        AulaPlanejamento aula = new AulaPlanejamento();
        aula.setDuracaoPeriodos(duracaoPeriodos);
        aula.setHorarioInicial(new HorarioPlanejamento(diaSemana, periodoInicial, turno));
        return aula;
    }

    private AulaPlanejamento aulaComHorario(
            Turma turma,
            Disciplina disciplina,
            Professor professor,
            int diaSemana,
            int turno,
            int periodoInicial) {
        return aulaComHorario(turma, disciplina, professor, diaSemana, turno, periodoInicial, 1);
    }

    private AulaPlanejamento aulaComHorario(
            Turma turma,
            Disciplina disciplina,
            Professor professor,
            int diaSemana,
            int turno,
            int periodoInicial,
            int duracaoPeriodos) {
        AulaPlanejamento aula = new AulaPlanejamento();
        aula.setId("Aula-" + diaSemana + "-" + turno + "-" + periodoInicial + "-" + duracaoPeriodos);
        aula.setTurma(turma);
        aula.setDisciplina(disciplina);
        aula.setProfessor(professor);
        aula.setDuracaoPeriodos(duracaoPeriodos);
        aula.setHorarioInicial(new HorarioPlanejamento(diaSemana, periodoInicial, turno));
        return aula;
    }

    private GradePlanejamento resolverHardScore(List<AulaPlanejamento> aulas, Professor... professores) {
        List<HorarioPlanejamento> horarios = aulas.stream()
                .map(AulaPlanejamento::getHorarioInicial)
                .toList();

        SolverConfig config = new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withConstraintProviderClass(GradeConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(Duration.ofSeconds(1)));

        return SolverFactory.<GradePlanejamento>create(config)
                .buildSolver()
                .solve(new GradePlanejamento(aulas, horarios, Arrays.asList(professores), null));
    }

    private Disciplina criarDisciplina(
            int id, String nome, int periodos, TamanhoGrade tamanhoGrade) {
        return new Disciplina(
                id,
                nome,
                "Geral",
                periodos * tamanhoGrade.getTamanhoPeriodo() / 60.0,
                tamanhoGrade,
                null,
                "comum");
    }

    private Turma criarTurma(
            int id,
            String nome,
            String turno,
            List<Disciplina> disciplinas,
            TamanhoGrade tamanhoGrade) {
        Curso curso = new Curso("Curso " + nome, "Regular", turno);
        return new Turma(id, nome, disciplinas, curso, tamanhoGrade);
    }

    private Professor criarProfessor(
            String nome, Boolean[][][] disponibilidade, Disciplina... disciplinas) {
        Professor professor = new Professor();
        professor.setNome(nome);
        professor.setDisponibilidadeHoraria(disponibilidade);
        professor.setDisciplinasPossiveis(Arrays.asList(disciplinas));
        return professor;
    }

    private Boolean[][][] disponibilidade(int diasMatutino, int diasVespertino) {
        return disponibilidade(diasMatutino, diasVespertino, 0);
    }

    private Boolean[][][] disponibilidade(int diasMatutino, int diasVespertino, int diasNoturno) {
        int diasTotais = Math.max(Math.max(Math.max(diasMatutino, diasVespertino), diasNoturno), DIAS_LETIVOS);
        Boolean[][][] disponibilidade = new Boolean[3][PERIODOS_POR_DIA][diasTotais];
        for (Boolean[][] turno : disponibilidade) {
            for (Boolean[] periodo : turno) {
                Arrays.fill(periodo, false);
            }
        }
        preencherTurno(disponibilidade, 0, diasMatutino);
        preencherTurno(disponibilidade, 1, diasVespertino);
        preencherTurno(disponibilidade, 2, diasNoturno);
        return disponibilidade;
    }

    private void preencherTurno(Boolean[][][] disponibilidade, int turno, int diasDisponiveis) {
        for (int periodo = 0; periodo < PERIODOS_POR_DIA; periodo++) {
            for (int dia = 0; dia < Math.min(diasDisponiveis, disponibilidade[turno][periodo].length); dia++) {
                disponibilidade[turno][periodo][dia] = true;
            }
        }
    }
}