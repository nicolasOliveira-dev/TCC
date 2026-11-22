package com.example.EduGrid.model;

import java.util.List;
import java.text.Normalizer;
import java.util.Locale;

/** Matriz pronta para exibir horarios por turno, periodo e dia. */
public class GradeSemanalVisualizacao {
    private final List<String> dias;
    private final List<String> turnos;
    private final List<Integer> periodos;
    private final String[][][] celulas;

    private GradeSemanalVisualizacao(List<String> dias, List<String> turnos,
            List<Integer> periodos, String[][][] celulas) {
        this.dias = dias;
        this.turnos = turnos;
        this.periodos = periodos;
        this.celulas = celulas;
    }

    public static GradeSemanalVisualizacao de(List<Horario> horarios, TamanhoGrade tamanho) {
        int quantidadeDias = tamanho == null ? 5 : tamanho.getQtdeDiasSemana();
        int quantidadePeriodos = tamanho == null ? 5 : tamanho.getQtdePeriodosDia();
        int maiorTurno = Math.max(2, (tamanho == null ? 1 : tamanho.getQtdeTurnos()) - 1);
        for (Horario h : horarios) {
            quantidadeDias = Math.max(quantidadeDias, h.getDiaSemana() + 1);
            quantidadePeriodos = Math.max(quantidadePeriodos, h.getPeriodoDia() + 1);
            maiorTurno = Math.max(maiorTurno, h.getTurno());
        }
        quantidadeDias = Math.min(7, quantidadeDias);
        quantidadePeriodos = Math.min(12, quantidadePeriodos);
        maiorTurno = Math.min(2, maiorTurno);
        var nomesDias = List.of("Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
                "Sexta-feira", "Sábado", "Domingo").subList(0, quantidadeDias);
        var nomesTurnos = List.of("Manhã", "Tarde", "Noite").subList(0, maiorTurno + 1);
        var listaPeriodos = java.util.stream.IntStream.rangeClosed(1, quantidadePeriodos).boxed().toList();
        var matriz = new String[maiorTurno + 1][quantidadePeriodos][quantidadeDias];
        for (int turno = 0; turno < matriz.length; turno++) {
            for (int periodo = 0; periodo < quantidadePeriodos; periodo++) {
                java.util.Arrays.fill(matriz[turno][periodo], "—");
            }
        }
        for (Horario h : horarios) {
            int turno = h.getTurno();
            int periodo = h.getPeriodoDia();
            int dia = h.getDiaSemana();
            if (turno < 0 || turno >= matriz.length || periodo < 0 || periodo >= quantidadePeriodos
                    || dia < 0 || dia >= quantidadeDias) {
                continue;
            }
            String detalhe = (h.getDisciplina() == null ? "Disciplina não informada" : h.getDisciplina().getNome())
                    + (h.getTurma() == null ? "" : " · " + h.getTurma().getNome());
            if (h.getProfessor() != null) {
                detalhe += " · " + h.getProfessor().getNome();
            }
            matriz[turno][periodo][dia] = "—".equals(matriz[turno][periodo][dia])
                    ? detalhe : matriz[turno][periodo][dia] + "\n" + detalhe;
        }
        return new GradeSemanalVisualizacao(nomesDias, nomesTurnos, listaPeriodos, matriz);
    }

    /** Creates a one-shift view for a class schedule. */
    public static GradeSemanalVisualizacao deTurno(List<Horario> horarios, TamanhoGrade tamanho, int turno) {
        GradeSemanalVisualizacao completa = de(horarios, tamanho);
        if (turno < 0 || turno >= completa.celulas.length) {
            turno = 0;
        }
        return new GradeSemanalVisualizacao(completa.dias,
                List.of(completa.turnos.get(turno)), completa.periodos,
                new String[][][] { completa.celulas[turno] });
    }

    /** Maps course turn names to the 0=morning, 1=afternoon, 2=evening convention. */
    public static int indiceDoTurno(String turno) {
        if (turno == null || turno.isBlank()) return -1;
        String normalizado = Normalizer.normalize(turno, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
        if (normalizado.contains("manha") || normalizado.contains("matut") || normalizado.equals("m")) return 0;
        if (normalizado.contains("tarde") || normalizado.contains("vespert") || normalizado.equals("t")) return 1;
        if (normalizado.contains("noite") || normalizado.contains("noturn") || normalizado.equals("n")) return 2;
        return -1;
    }

    public List<String> getDias() { return dias; }
    public List<String> getTurnos() { return turnos; }
    public List<Integer> getPeriodos() { return periodos; }
    public String[][][] getCelulas() { return celulas; }
}
