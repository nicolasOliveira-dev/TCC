package com.example.EduGrid.solver.domain;

import java.text.Normalizer;
import java.util.Locale;

public class HorarioPlanejamento {

    private int diaSemana;
    private int turno;
    private int periodoInicial;
    private int indiceTurnoDisponibilidade;

    public HorarioPlanejamento() {
    }

    public HorarioPlanejamento(int diaSemana, int periodoInicial, int turno) {
        this(diaSemana, periodoInicial, turno, turno);
    }

    public HorarioPlanejamento(
            int diaSemana,
            int periodoInicial,
            int turno,
            int indiceTurnoDisponibilidade) {
        this.diaSemana = diaSemana;
        this.periodoInicial = periodoInicial;
        this.turno = turno;
        this.indiceTurnoDisponibilidade = indiceTurnoDisponibilidade;
    }

    // Construtores, getters e setters

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public int getTurno() {
        return turno;
    }

    public void setTurno(int turno) {
        this.turno = turno;
    }

    public int getPeriodoInicial() {
        return periodoInicial;
    }

    public void setPeriodoInicial(int periodoInicial) {
        this.periodoInicial = periodoInicial;
    }

    public int getIndiceTurnoDisponibilidade() {
        return indiceTurnoDisponibilidade;
    }

    public void setIndiceTurnoDisponibilidade(int indiceTurnoDisponibilidade) {
        this.indiceTurnoDisponibilidade = indiceTurnoDisponibilidade;
    }

    public static int indiceDoTurno(String nome) {
        if (nome == null) {
            throw new IllegalArgumentException("O turno da turma é obrigatório.");
        }
        String normalizado = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (normalizado.contains("matutino") || normalizado.contains("manha")) {
            return 0;
        }
        if (normalizado.contains("vespertino") || normalizado.contains("tarde")) {
            return 1;
        }
        if (normalizado.contains("noturno") || normalizado.contains("noite")) {
            return 2;
        }
        throw new IllegalArgumentException("Turno desconhecido: " + nome);
    }
}
