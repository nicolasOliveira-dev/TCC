package com.example.EduGrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;

class GradeHorariaTest {

    @Test
    void deveGerarEImprimirMatrizParaDezTurmas() {
        TamanhoGrade tamanho = new TamanhoGrade(1, 6, 5);

        List<Turma> turmas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            turmas.add(new Turma());
        }

        GradeHoraria grade = new GradeHoraria(turmas, tamanho);
        grade.gerarMatriz();

        Object[][] matriz = grade.getMatriz();

        assertNotNull(matriz);
        assertEquals(10, matriz.length, "A matriz deve ter uma linha por turma.");
        assertEquals(30, matriz[0].length, "Cada turma deve ter 30 períodos.");

        System.out.println("\nFormato da matriz da grade horária:");
        for (int i = 0; i < matriz.length; i++) {
            System.out.printf("Turma %d: %s%n", i + 1, Arrays.toString(matriz[i]));
        }
    }
}