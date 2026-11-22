package com.example.EduGrid.solver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;

import ai.timefold.solver.core.api.score.HardSoftScore;

class DiagnosticoOtimizacaoTest {

    @Test
    void retornaPontuacaoHardSoftEBlocosEnvolvidosSemMudarAGrade() {
        TamanhoGrade tamanho = new TamanhoGrade(3, 6, 5);
        Disciplina matematica = disciplina(1, "Matemática");
        Disciplina fisica = disciplina(2, "Física");
        Disciplina quimica = disciplina(3, "Química");
        Curso curso = new Curso("Curso teste", "Técnico", "matutino");
        curso.setId(1);
        Turma turma = new Turma(1, "Turma A", List.of(matematica, fisica), curso, tamanho, 1);
        Curso outroCurso = new Curso("Outro curso", "Técnico", "matutino");
        outroCurso.setId(2);
        Turma outraTurma = new Turma(2, "Turma B", List.of(quimica), outroCurso, tamanho, 1);
        Professor professor1 = professor(matematica);
        Professor professor2 = professor(fisica);
        Professor professor3 = professor(quimica);
        professor3.setIntencaoDiaPlanejamento(List.of());
        HorarioPlanejamento horario = new HorarioPlanejamento(4, 0, 0);
        AulaPlanejamento aulaMatematica = aula("aula-mat", turma, matematica, professor1, horario);
        AulaPlanejamento aulaFisica = aula("aula-fis", turma, fisica, professor2, horario);
        AulaPlanejamento aulaOutraTurma = aula("aula-outra-turma", outraTurma, quimica, professor3, horario);
        GradePlanejamento grade = new GradePlanejamento(
                List.of(aulaMatematica, aulaFisica, aulaOutraTurma),
                List.of(horario),
                List.of(professor1, professor2, professor3));
        grade.setTurmas(List.of(turma, outraTurma));
        HardSoftScore pontuacaoOriginal = grade.getScore();

        DiagnosticoGrade resultado = DiagnosticoOtimizacao.analisar(grade);

        assertEquals(HardSoftScore.of(-1, -2), resultado.pontuacao());
        assertEquals(pontuacaoOriginal, grade.getScore(),
                "A análise deve manter intacta a solução recebida.");
        assertFalse(resultado.viavel());

        DiagnosticoGrade.Violacao conflitoTurma = resultado.violacoes().stream()
                .filter(v -> v.restricao().equals("Conflito de turma"))
                .findFirst()
                .orElseThrow();
        assertEquals(DiagnosticoGrade.Severidade.HARD, conflitoTurma.severidade());
        assertEquals(HardSoftScore.ofHard(-1), conflitoTurma.impacto());
        assertEquals(1, conflitoTurma.quantidadeViolacoes());
        assertEquals(2, conflitoTurma.blocosEnvolvidos());
        assertTrue(conflitoTurma.exemplos().stream().anyMatch(s -> s.contains("aula-mat")));
        assertTrue(conflitoTurma.exemplos().stream().anyMatch(s -> s.contains("aula-fis")));
        assertFalse(conflitoTurma.exemplos().stream().anyMatch(s -> s.contains("aula-outra-turma")),
                "Aula simultânea de outra turma não deve aparecer no conflito da turma A.");
        assertEquals(2, conflitoTurma.blocosEnvolvidos(),
                "Só os dois blocos da mesma turma devem ser apontados como envolvidos.");

        DiagnosticoGrade.Violacao preferencia = resultado.violacoes().stream()
                .filter(v -> v.restricao().equals("Aula no dia de planejamento preferido"))
                .findFirst()
                .orElseThrow();
        assertEquals(DiagnosticoGrade.Severidade.SOFT, preferencia.severidade());
        assertEquals(HardSoftScore.ofSoft(-2), preferencia.impacto());
        assertEquals(2, preferencia.quantidadeViolacoes());
    }

    private Disciplina disciplina(int id, String nome) {
        Disciplina disciplina = new Disciplina(id, nome, "Exatas", 1.0,
                null, null, "comum");
        return disciplina;
    }

    private Professor professor(Disciplina disciplina) {
        Boolean[][][] disponibilidade = new Boolean[3][6][5];
        for (Boolean[][] turno : disponibilidade) {
            for (Boolean[] periodo : turno) {
                Arrays.fill(periodo, Boolean.TRUE);
            }
        }
        Professor professor = new Professor();
        professor.setNome("Docente " + disciplina.getNome());
        professor.setDisciplinasPossiveis(List.of(disciplina));
        professor.setDisponibilidadeHoraria(disponibilidade);
        professor.setIntencaoDiaPlanejamento(List.of("Sexta-feira"));
        return professor;
    }

    private AulaPlanejamento aula(
            String id,
            Turma turma,
            Disciplina disciplina,
            Professor professor,
            HorarioPlanejamento horario) {
        AulaPlanejamento aula = new AulaPlanejamento(
                id, turma, disciplina, 1, professor, horario);
        return aula;
    }
}
