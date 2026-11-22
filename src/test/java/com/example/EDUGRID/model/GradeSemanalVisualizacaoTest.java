package com.example.EduGrid.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class GradeSemanalVisualizacaoTest {

    @Test
    void exibeSomenteOTurnoSelecionadoParaTurma() {
        var manha = new Disciplina(1, "Aula da manhã", "Geral", 1.0, null, null, "comum");
        var tarde = new Disciplina(2, "Aula da tarde", "Geral", 1.0, null, null, "comum");
        var horarios = List.of(
                new Horario(1, 0, 0, 0, null, manha, null),
                new Horario(2, 1, 0, 0, null, tarde, null));

        var grade = GradeSemanalVisualizacao.deTurno(horarios,
                new TamanhoGrade(3, 5, 5), 1);

        assertEquals(List.of("Tarde"), grade.getTurnos());
        assertEquals("Aula da tarde", grade.getCelulas()[0][0][0]);
    }

    @Test
    void reconheceNomesDeTurnoDoCurso() {
        assertEquals(0, GradeSemanalVisualizacao.indiceDoTurno("Matutino"));
        assertEquals(1, GradeSemanalVisualizacao.indiceDoTurno("Vespertino"));
        assertEquals(2, GradeSemanalVisualizacao.indiceDoTurno("Noturno"));
        assertEquals(-1, GradeSemanalVisualizacao.indiceDoTurno("Integral"));
    }
}
