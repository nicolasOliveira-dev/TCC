package com.example.EduGrid.solver.domain;

public final class RegraHorarioEscolar {

    private static final int QUARTA_FEIRA = 2;
    private static final int TURNO_MATUTINO = 0;
    private static final int TURNO_VESPERTINO = 1;

    private RegraHorarioEscolar() {
    }

   
    public static boolean ocupaPeriodoProibidoNaQuarta(
            HorarioPlanejamento horario,
            int duracaoPeriodos,
            int quantidadePeriodosDia) {
        if (horario == null || duracaoPeriodos <= 0 || quantidadePeriodosDia <= 0
                || horario.getDiaSemana() != QUARTA_FEIRA
                || (horario.getTurno() != TURNO_MATUTINO
                && horario.getTurno() != TURNO_VESPERTINO)) {
            return false;
        }

        int primeiroPeriodoProibido = Math.max(0, quantidadePeriodosDia - 2);
        int inicioBloco = horario.getPeriodoInicial();
        int fimBloco = inicioBloco + duracaoPeriodos;
        return inicioBloco < quantidadePeriodosDia && fimBloco > primeiroPeriodoProibido;
    }


    public static int periodosIndisponiveisNaQuarta(
            int turno,
            int quantidadeDiasSemana,
            int quantidadePeriodosDia) {
        if (quantidadeDiasSemana <= QUARTA_FEIRA
                || (turno != TURNO_MATUTINO && turno != TURNO_VESPERTINO)
                || quantidadePeriodosDia <= 0) {
            return 0;
        }
        return Math.min(2, quantidadePeriodosDia);
    }
}
