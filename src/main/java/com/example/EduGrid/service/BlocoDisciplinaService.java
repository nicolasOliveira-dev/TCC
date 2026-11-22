package com.example.EduGrid.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.TamanhoGrade;

@Service
public class BlocoDisciplinaService {

    public List<Integer> calcularBlocos(Disciplina disciplina, TamanhoGrade tamanhoGrade) {
        Configuracao configuracao = obterConfiguracao(disciplina, tamanhoGrade);
        int restantes = configuracao.periodosNecessarios();
        if (restantes == 0) {
            return List.of();
        }

        int blocosRestantes = 1 + (restantes - 1) / configuracao.maximo();
        if ((long) restantes < (long) blocosRestantes * configuracao.minimo()) {
            throw new IllegalStateException(
                    "Não é possível dividir os períodos da disciplina: " + disciplina.getNome());
        }

        List<Integer> blocos = new ArrayList<>(blocosRestantes);
        while (restantes > 0) {
            List<Integer> tamanhosCandidatos = calcularTamanhosCandidatos(
                    disciplina,
                    tamanhoGrade,
                    restantes,
                    blocosRestantes
            );
            int tamanhoBloco = tamanhosCandidatos.get(0);
            blocos.add(tamanhoBloco);
            restantes -= tamanhoBloco;
            blocosRestantes--;
        }
        return List.copyOf(blocos);
    }

    List<Integer> calcularTamanhosCandidatos(
            Disciplina disciplina,
            TamanhoGrade tamanhoGrade,
            int periodosRestantes,
            int blocosRestantes) {
        Configuracao configuracao = obterConfiguracao(disciplina, tamanhoGrade);
        if (periodosRestantes < 0 || blocosRestantes < 0) {
            throw new IllegalArgumentException("Os períodos e blocos restantes não podem ser negativos.");
        }
        if (periodosRestantes == 0 && blocosRestantes == 0) {
            return List.of();
        }
        if (periodosRestantes == 0 || blocosRestantes == 0) {
            throw new IllegalStateException(
                    "Não é possível distribuir os períodos restantes da disciplina: " + disciplina.getNome());
        }
        if ((long) periodosRestantes < (long) blocosRestantes * configuracao.minimo()
                || (long) periodosRestantes > (long) blocosRestantes * configuracao.maximo()) {
            throw new IllegalStateException(
                    "Não é possível distribuir os períodos restantes da disciplina: " + disciplina.getNome());
        }

        int blocosAposAtual = blocosRestantes - 1;
        int tamanhoMaximoAtual = Math.min(
                configuracao.maximo(),
                (int) (periodosRestantes - (long) blocosAposAtual * configuracao.minimo())
        );
        int tamanhoMinimoAtual = Math.max(
                configuracao.minimo(),
                (int) (periodosRestantes - (long) blocosAposAtual * configuracao.maximo())
        );
        if (tamanhoMaximoAtual < tamanhoMinimoAtual) {
            throw new IllegalStateException(
                    "Não foi possível definir os blocos da disciplina: " + disciplina.getNome());
        }

        List<Integer> tamanhosCandidatos = new ArrayList<>();
        for (int tamanho = tamanhoMaximoAtual; tamanho >= tamanhoMinimoAtual; tamanho--) {
            tamanhosCandidatos.add(tamanho);
        }
        return List.copyOf(tamanhosCandidatos);
    }

    private Configuracao obterConfiguracao(Disciplina disciplina, TamanhoGrade tamanhoGrade) {
        if (disciplina == null || tamanhoGrade == null) {
            throw new IllegalArgumentException("Disciplina e tamanho da grade são obrigatórios.");
        }

        int periodosNecessarios = disciplina.getNumPeriodos();
        int minimo = disciplina.getMinPeriodosJuntos();
        int maximoConfigurado = disciplina.getMaxPeriodosJuntos();
        int maximo = Math.min(maximoConfigurado, tamanhoGrade.getQtdePeriodosDia());
        if (periodosNecessarios < 0 || minimo <= 0 || maximo < minimo) {
            throw new IllegalArgumentException(
                    "Configuração inválida de períodos consecutivos para a disciplina: " + disciplina.getNome());
        }
        return new Configuracao(periodosNecessarios, minimo, maximo);
    }

    private record Configuracao(int periodosNecessarios, int minimo, int maximo) {

    }
}
