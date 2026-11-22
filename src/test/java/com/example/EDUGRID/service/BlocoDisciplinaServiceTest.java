package com.example.EduGrid.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.TamanhoGrade;

class BlocoDisciplinaServiceTest {

    private final BlocoDisciplinaService service = new BlocoDisciplinaService();

    @Test
    void calculaSeisPeriodosComoQuatroMaisDois() {
        Disciplina disciplina = criarDisciplina(6, 2, 4, 5);

        assertEquals(List.of(4, 2), service.calcularBlocos(
                disciplina,
                disciplina.getTamanhoGrade()
        ));
    }

    @Test
    void calculaOitoPeriodosComoDoisBlocosDeQuatro() {
        Disciplina disciplina = criarDisciplina(8, 2, 4, 5);

        assertEquals(List.of(4, 4), service.calcularBlocos(
                disciplina,
                disciplina.getTamanhoGrade()
        ));
    }

    @Test
    void limitaBlocoAoNumeroDePeriodosDisponiveisNoDia() {
        Disciplina disciplina = criarDisciplina(6, 2, 4, 3);

        assertEquals(List.of(3, 3), service.calcularBlocos(
                disciplina,
                disciplina.getTamanhoGrade()
        ));
    }

    @Test
    void calculaCandidatosEmOrdemDecrescenteParaPreservarFallbackDeAlocacao() {
        Disciplina disciplina = criarDisciplina(6, 2, 4, 5);

        assertEquals(
                List.of(4, 3, 2),
                service.calcularTamanhosCandidatos(
                        disciplina,
                        disciplina.getTamanhoGrade(),
                        6,
                        2
                )
        );
        assertEquals(
                List.of(3),
                service.calcularTamanhosCandidatos(
                        disciplina,
                        disciplina.getTamanhoGrade(),
                        3,
                        1
                )
        );
    }

    @Test
    void rejeitaConfiguracaoQueNaoPermiteDistribuirOsPeriodos() {
        Disciplina disciplina = criarDisciplina(5, 3, 4, 5);

        assertThrows(IllegalStateException.class, () -> service.calcularBlocos(
                disciplina,
                disciplina.getTamanhoGrade()
        ));
    }

    @Test
    void rejeitaLimitesDeDuracaoInvalidos() {
        Disciplina disciplina = criarDisciplina(6, 2, 1, 5);

        assertThrows(IllegalArgumentException.class, () -> service.calcularBlocos(
                disciplina,
                disciplina.getTamanhoGrade()
        ));
    }

    @Test
    void retornaListaVaziaQuandoDisciplinaNaoPossuiPeriodos() {
        Disciplina disciplina = criarDisciplina(0, 2, 4, 5);

        assertEquals(List.of(), service.calcularBlocos(
                disciplina,
                disciplina.getTamanhoGrade()
        ));
    }

    private Disciplina criarDisciplina(
            int periodos, int minimo, int maximo, int periodosPorDia) {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, periodosPorDia, 5);
        Disciplina disciplina = new Disciplina(
                1,
                "Disciplina de teste",
                "Geral",
                periodos * tamanhoGrade.getTamanhoPeriodo() / 60.0,
                tamanhoGrade,
                null,
                "comum"
        );
        disciplina.setMinPeriodosJuntos(minimo);
        disciplina.setMaxPeriodosJuntos(maximo);
        return disciplina;
    }
}
