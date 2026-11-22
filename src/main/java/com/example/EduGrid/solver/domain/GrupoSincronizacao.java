package com.example.EduGrid.solver.domain;

import java.util.List;
import java.util.Map;

public record GrupoSincronizacao(
        String id,
        int totalPeriodos,
        Map<Integer, List<AulaPlanejamento>> blocosPorTurma
) {
}
