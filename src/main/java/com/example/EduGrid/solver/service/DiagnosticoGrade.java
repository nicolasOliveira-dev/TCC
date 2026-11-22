package com.example.EduGrid.solver.service;

import java.util.List;

import ai.timefold.solver.core.api.score.HardSoftScore;

/** Resultado estruturado da análise das restrições de uma grade. */
public record DiagnosticoGrade(
        HardSoftScore pontuacao,
        boolean viavel,
        List<Violacao> violacoes) {

    public DiagnosticoGrade {
        violacoes = List.copyOf(violacoes);
    }

    public record Violacao(
            String restricao,
            Severidade severidade,
            HardSoftScore impacto,
            Integer quantidadeViolacoes,
            int blocosEnvolvidos,
            List<String> exemplos) {

        public Violacao {
            exemplos = List.copyOf(exemplos);
        }
    }

    public enum Severidade {
        HARD,
        SOFT
    }
}
