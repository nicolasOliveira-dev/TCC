package com.example.EduGrid.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.EduGrid.repository.TurmaRepository;
import com.example.EduGrid.service.TurmaService;

class GradeHorariaConfigurationTest {

    @Test
    void generatingGradeResizesEachClassUsingLatestConfiguration() {
        TamanhoGrade configuracaoAnterior = new TamanhoGrade(2, 5, 5);
        TamanhoGrade configuracaoAtual = new TamanhoGrade(1, 4, 3);
        Turma turma = new Turma(1, "Turma A", List.of(), null, configuracaoAnterior);
        GradeHoraria grade = new GradeHoraria(List.of(turma), configuracaoAtual);

        TurmaService turmaService = new TurmaService(Mockito.mock(TurmaRepository.class));
        turmaService.alocarGradeHoraria(turma, configuracaoAtual);
        grade.gerarMatriz();

        assertSame(configuracaoAtual, turma.getTamanhoGrade());
        assertEquals(3, turma.getGradeHorariaMatriz().length);
        assertEquals(4, turma.getGradeHorariaMatriz()[0].length);
        assertEquals(12, grade.getMatriz()[0].length);
    }

    @Test
    void generatingGradeDisplaysDisciplineNamesInAllocatedPeriods() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 4, 3);
        Disciplina disciplina = new Disciplina();
        disciplina.setId(10);
        disciplina.setNome("Matemática");
        disciplina.setCargaHoraria(2);
        Turma turma = new Turma(1, "Turma A", List.of(disciplina), null, tamanhoGrade);
        GradeHoraria grade = new GradeHoraria(List.of(turma), tamanhoGrade);
        TurmaService turmaService = new TurmaService(Mockito.mock(TurmaRepository.class));

        turmaService.alocarGradeHoraria(turma, tamanhoGrade);
        grade.gerarMatriz();

        assertEquals(3, disciplina.getNumPeriodos());
        assertEquals("Matemática", grade.getMatriz()[0][0]);
        assertEquals("Matemática", grade.getMatriz()[0][1]);
        assertEquals("Matemática", grade.getMatriz()[0][2]);
        assertNull(grade.getMatriz()[0][3]);
    }
}
