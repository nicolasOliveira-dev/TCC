package com.example.EduGrid.solver.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.example.EduGrid.model.*;
import com.example.EduGrid.solver.domain.*;
import com.example.EduGrid.solver.constraint.GradeConstraintProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.*;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

class PreparacaoOtimizacaoTest {
    private GradePlanejamento problema() {
        var tamanho = new TamanhoGrade(1, 3, 5);
        var disciplina = new Disciplina(1, "Matematica", "Geral", 1.5, tamanho, null, "comum");
        var turma = new Turma(1, "Turma A", List.of(disciplina),
                new Curso("Curso", "Tecnico", "matutino"), tamanho);
        var professor = new Professor();
        professor.setNome("Ana");
        professor.setDisciplinasPossiveis(List.of(disciplina));
        professor.setDisponibilidadeHoraria(new Boolean[][][]{{
                {true, false, false, false, false},
                {true, false, false, false, false},
                {false, false, false, false, false}}});
        var outro = new Professor();
        outro.setNome("Sem habilitacao");
        var aula = new AulaPlanejamento("1", turma, disciplina, 2, null, null);
        var grade = new GradePlanejamento(List.of(aula), List.of(
                new HorarioPlanejamento(0, 0, 0),
                new HorarioPlanejamento(0, 1, 0),
                new HorarioPlanejamento(0, 2, 0),
                new HorarioPlanejamento(0, 0, 1)), List.of(professor, outro));
        grade.setTurmas(List.of(turma));
        return grade;
    }

    @Test void restringeCandidatosESolverEncontraSolucaoValida() {
        var grade = problema();
        PreparacaoOtimizacao.preparar(grade);
        var aula = grade.getAulas().getFirst();
        assertEquals(List.of(grade.getProfessores().getFirst()), aula.getProfessoresCandidatos());
        assertEquals(List.of(grade.getHorariosPossiveis().getFirst()), aula.getHorariosCandidatos());
        var resultado = fabrica().buildSolver().solve(grade);
        assertTrue(resultado.getScore().isFeasible());
        assertEquals("Ana", resultado.getAulas().getFirst().getProfessor().getNome());
    }

    @Test void rejeitaFaltaDeHabilitacao() {
        var grade = problema();
        grade.getProfessores().getFirst().setDisciplinasPossiveis(List.of());
        var erro = assertThrows(DiagnosticoOtimizacaoException.class, () -> PreparacaoOtimizacao.preparar(grade));
        assertTrue(erro.getMessage().contains("nenhum professor habilitado"));
    }

    @Test void rejeitaDisponibilidadeParcialDoBloco() {
        var grade = problema();
        grade.getProfessores().getFirst().getDisponibilidadeHoraria()[0][1][0] = false;
        var erro = assertThrows(DiagnosticoOtimizacaoException.class, () -> PreparacaoOtimizacao.preparar(grade));
        assertTrue(erro.getMessage().contains("bloco de 2 periodos"));
    }

    @Test void rejeitaCargaMaiorQueCapacidade() {
        var grade = problema();
        grade.getAulas().getFirst().setDuracaoPeriodos(16);
        var erro = assertThrows(DiagnosticoOtimizacaoException.class, () -> PreparacaoOtimizacao.preparar(grade));
        assertTrue(erro.getMessage().contains("16 periodos obrigatorios para apenas 13 vagas semanais"));
    }

    @Test void descontaVagasDaQuartaNaCapacidadeSemanalDaManhaETarde() {
        var grade = problema();
        grade.getAulas().getFirst().setDuracaoPeriodos(14);

        var erro = assertThrows(DiagnosticoOtimizacaoException.class,
                () -> PreparacaoOtimizacao.preparar(grade));

        assertTrue(erro.getMessage().contains("14 periodos obrigatorios para apenas 13 vagas semanais"));
    }

    @Test void naoDescontaVagasDaQuartaNaCapacidadeSemanalNoturna() {
        var grade = problema();
        grade.getAulas().getFirst().getTurma().setCurso(new Curso("Curso", "Tecnico", "noturno"));
        grade.getAulas().getFirst().setDuracaoPeriodos(16);

        var erro = assertThrows(DiagnosticoOtimizacaoException.class,
                () -> PreparacaoOtimizacao.preparar(grade));

        assertTrue(erro.getMessage().contains("16 periodos obrigatorios para apenas 15 vagas semanais"));
    }

    @Test void removeDosCandidatosOsBlocosQueInvademOsDoisUltimosPeriodosDaQuarta() {
        var grade = problema();
        var aula = grade.getAulas().getFirst();
        aula.getTurma().getTamanhoGrade().setQtdePeriodosDia(6);
        var disponibilidade = new Boolean[3][6][5];
        for (Boolean[][] turno : disponibilidade) {
            for (Boolean[] periodo : turno) {
                Arrays.fill(periodo, Boolean.TRUE);
            }
        }
        grade.getProfessores().getFirst().setDisponibilidadeHoraria(disponibilidade);

        List<HorarioPlanejamento> horarios = new ArrayList<>();
        for (int dia = 0; dia < 5; dia++) {
            for (int periodo = 0; periodo < 6; periodo++) {
                horarios.add(new HorarioPlanejamento(dia, periodo, 0));
            }
        }
        grade.setHorariosPossiveis(horarios);

        PreparacaoOtimizacao.preparar(grade);

        assertTrue(aula.getHorariosCandidatos().stream()
                .anyMatch(h -> h.getDiaSemana() == 2 && h.getPeriodoInicial() == 2));
        assertFalse(aula.getHorariosCandidatos().stream()
                .anyMatch(h -> h.getDiaSemana() == 2 && h.getPeriodoInicial() >= 3));
    }

    @Test void mantemCandidatosDoTurnoNoturnoNaQuarta() {
        var grade = problema();
        var aula = grade.getAulas().getFirst();
        aula.getTurma().setCurso(new Curso("Curso", "Tecnico", "noturno"));
        aula.getTurma().getTamanhoGrade().setQtdeTurnos(3);
        aula.getTurma().getTamanhoGrade().setQtdePeriodosDia(6);
        var disponibilidade = new Boolean[3][6][5];
        for (Boolean[][] turno : disponibilidade) {
            for (Boolean[] periodo : turno) {
                Arrays.fill(periodo, Boolean.TRUE);
            }
        }
        grade.getProfessores().getFirst().setDisponibilidadeHoraria(disponibilidade);
        List<HorarioPlanejamento> horarios = new ArrayList<>();
        for (int periodo = 0; periodo < 6; periodo++) {
            horarios.add(new HorarioPlanejamento(2, periodo, 2, 2));
        }
        grade.setHorariosPossiveis(horarios);

        PreparacaoOtimizacao.preparar(grade);

        assertEquals(5, aula.getHorariosCandidatos().size());
        assertTrue(aula.getHorariosCandidatos().stream()
                .anyMatch(h -> h.getPeriodoInicial() == 4));
    }

    @Test void diagnosticoIdentificaRegraEAula() {
        var grade = problema();
        var aula = grade.getAulas().getFirst();
        aula.setProfessor(grade.getProfessores().getFirst());
        aula.setHorarioInicial(grade.getHorariosPossiveis().get(1));
        SolutionManager<GradePlanejamento, HardSoftScore> manager = SolutionManager.create(fabrica());
        manager.update(grade);
        var scoreOriginal = grade.getScore();
        var mensagem = DiagnosticoOtimizacao.descrever(grade);
        assertEquals(scoreOriginal, grade.getScore());
        assertTrue(mensagem.contains("Professor indisponivel no horario"));
        assertTrue(mensagem.contains("Turma A / Matematica"));
        assertTrue(mensagem.contains("Ana"));
    }

    @Test void diagnosticoRegrasQuartaMesmoQuandoExecutadoEmThreadComOutroContexto() {
        var grade = problema();
        var aula = grade.getAulas().getFirst();
        aula.setProfessor(grade.getProfessores().getFirst());
        var horarios = new ArrayList<>(grade.getHorariosPossiveis());
        var horarioQuarta = new HorarioPlanejamento(2, 1, 0);
        horarios.add(horarioQuarta);
        grade.setHorariosPossiveis(horarios);
        aula.setCandidatos(grade.getProfessores(), horarios);
        aula.setHorarioInicial(horarioQuarta);
        grade.setScore(HardSoftScore.of(-1, -30));

        Thread thread = Thread.currentThread();
        ClassLoader contextoOriginal = thread.getContextClassLoader();
        ClassLoader contextoAlternativo = contextoOriginal == null
                ? ClassLoader.getSystemClassLoader().getParent()
                : contextoOriginal.getParent();
        thread.setContextClassLoader(contextoAlternativo);
        try {
            var mensagem = DiagnosticoOtimizacao.descrever(grade);

            assertTrue(mensagem.contains("Aulas nos dois ultimos periodos da quarta-feira"));
            assertTrue(mensagem.contains("Turma A / Matematica"));
            assertSame(contextoAlternativo, thread.getContextClassLoader());
        } finally {
            thread.setContextClassLoader(contextoOriginal);
        }
    }

    private SolverFactory<GradePlanejamento> fabrica() {
        return SolverFactory.create(new SolverConfig().withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class).withConstraintProviderClass(GradeConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withSpentLimit(Duration.ofMillis(100))));
    }
}
