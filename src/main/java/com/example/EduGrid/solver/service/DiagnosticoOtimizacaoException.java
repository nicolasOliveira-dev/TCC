package com.example.EduGrid.solver.service;

/** Mensagem de dominio que pode ser apresentada ao usuario. */
public class DiagnosticoOtimizacaoException extends IllegalStateException {
    public DiagnosticoOtimizacaoException(String mensagem) {
        super(mensagem);
    }
}
