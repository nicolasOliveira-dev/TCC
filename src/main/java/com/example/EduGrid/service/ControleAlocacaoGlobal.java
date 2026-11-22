package com.example.EduGrid.service;

import java.util.HashMap;
import java.util.Map;

public class ControleAlocacaoGlobal {

    private final Map<Integer, Map<String, boolean[][]>> ocupacaoGlobal = new HashMap<>();

    private final int dias;
    private final int periodos;

    public ControleAlocacaoGlobal(int dias, int periodos) {
        this.dias = dias;
        this.periodos = periodos;
    }

    private boolean[][] obterMatriz(String chaveDisciplina, int turno) {
        return ocupacaoGlobal
                .computeIfAbsent(turno, k -> new HashMap<>())
                .computeIfAbsent(chaveDisciplina, k -> new boolean[dias][periodos]);
    }

    public boolean estaDisponivel(String chaveDisciplina, int dia, int periodo, int turno) {
        return !obterMatriz(chaveDisciplina, turno)[dia][periodo];
    }

    public void registrar(String chaveDisciplina, int dia, int periodo, int turno) {
        obterMatriz(chaveDisciplina, turno)[dia][periodo] = true;

    }
}