package com.example.EduGrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.TurmaGrade;

class TurmaGradeTest {

    @Test
    void deveAlocarEImprimirGradeHorariaCompleta() {
        TamanhoGrade tamanho = new TamanhoGrade(1, 6, 5);
        String[] nomes = {
            "", "Matemática", "Português", "História",
            "Geografia", "Ciências", "Inglês"
        };

        List<Disciplina> disciplinas = new ArrayList<>();
        for (int id = 1; id < nomes.length; id++) {
            // 225 minutos / 45 minutos por período = 5 períodos.
            disciplinas.add(new Disciplina(id, nomes[id], 225, 0, "", ""));
        }

        TurmaGrade turma = new TurmaGrade(
                1, "Turma de teste", disciplinas, null, tamanho);

        turma.alocarGradeHoraria(disciplinas, tamanho);

        int[][] matriz = turma.getGradeHorariaMatriz();
        int[] periodosPorDisciplina = new int[nomes.length];

        System.out.println("\nGrade horária — " + turma.getNome());

        for (int dia = 0; dia < matriz.length; dia++) {
            System.out.printf("Dia %d: ", dia + 1);

            for (int periodo = 0; periodo < matriz[dia].length; periodo++) {
                int id = matriz[dia][periodo];

                assertTrue(id > 0 && id < nomes.length,
                        "Foi encontrado um período vazio ou inválido.");

                periodosPorDisciplina[id]++;
                System.out.printf("%s: %s%s",
                        periodo + 1,
                        nomes[id],
                        periodo == matriz[dia].length - 1 ? "" : " | ");
            }

            System.out.println();
        }

        for (int id = 1; id < periodosPorDisciplina.length; id++) {
            assertEquals(5, periodosPorDisciplina[id],
                    nomes[id] + " deve ocupar cinco períodos.");
        }
    }
}