package com.example.EduGrid.solver.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Turma;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.CandidatoSincronizacao;
import com.example.EduGrid.solver.domain.GrupoSincronizacao;
import com.example.EduGrid.solver.domain.VinculoSincronizacao;

@Service
public class IdentificacaoSincronizacaoService {
    private static final int MAX_SUBCONJUNTOS_POR_TOTAL = 3;

    public List<CandidatoSincronizacao> identificarCandidatos(
            List<AulaPlanejamento> aulas) {

        List<CandidatoSincronizacao> candidatos = new ArrayList<>();

        for (int i = 0; i < aulas.size(); i++) {

            AulaPlanejamento aula1 = aulas.get(i);

            if (!ehTecnica(aula1)) {
                continue;
            }

            for (int j = i + 1; j < aulas.size(); j++) {

                AulaPlanejamento aula2 = aulas.get(j);

                if (!ehTecnica(aula2)) {
                    continue;
                }

                Turma turma1 = aula1.getTurma();
                Turma turma2 = aula2.getTurma();

                if (turma1.getCurso() == null
                        || turma2.getCurso() == null) {
                    continue;
                }

                if (!Objects.equals(
                        turma1.getCurso().getId(),
                        turma2.getCurso().getId())) {
                    continue;
                }

                if (turma1.getAno() == null
                        || turma2.getAno() == null
                        || turma1.getAno().equals(turma2.getAno())) {
                    continue;
                }

                if (!Objects.equals(
                        turma1.getCurso().getTurno(),
                        turma2.getCurso().getTurno())) {
                    continue;
                }

                candidatos.add(
                        new CandidatoSincronizacao(aula1, aula2)
                );
            }
        }

        return candidatos;
    }

    public List<VinculoSincronizacao> identificarVinculosSincronizacao(
            List<GrupoSincronizacao> grupos) {

        List<VinculoSincronizacao> vinculos = new ArrayList<>();
        if (grupos == null || grupos.isEmpty()) {
            return vinculos;
        }

        for (GrupoSincronizacao grupo : grupos) {
            if (grupo == null || grupo.blocosPorTurma() == null) {
                continue;
            }

            for (Map.Entry<Integer, List<AulaPlanejamento>> entrada : grupo.blocosPorTurma().entrySet()) {
                if (entrada.getValue() == null) {
                    continue;
                }

                for (AulaPlanejamento aula : entrada.getValue()) {
                    if (aula == null || aula.getId() == null) {
                        continue;
                    }
                    vinculos.add(new VinculoSincronizacao(grupo.id(), entrada.getKey(), aula.getId()));
                }
            }
        }

        return vinculos;
    }

    public List<GrupoSincronizacao> identificarGruposSincronizacao(
            List<AulaPlanejamento> aulas) {

        if (aulas == null || aulas.isEmpty()) {
            return List.of();
        }

        Map<String, List<AulaPlanejamento>> blocosPorCursoTurnoTurma = new HashMap<>();
        for (AulaPlanejamento aula : aulas) {
            if (!ehTecnica(aula) || aula.getTurma() == null) {
                continue;
            }

            Turma turma = aula.getTurma();
            if (turma.getCurso() == null || turma.getId() == null || turma.getAno() == null) {
                continue;
            }

            String key = turma.getCurso().getId() + ":"
                    + turma.getCurso().getTurno() + ":"
                    + turma.getId();
            blocosPorCursoTurnoTurma.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(aula);
        }

        Map<String, List<Turma>> turmasPorCursoTurno = new HashMap<>();
        for (AulaPlanejamento aula : aulas) {
            if (!ehTecnica(aula) || aula.getTurma() == null) {
                continue;
            }

            Turma turma = aula.getTurma();
            if (turma.getCurso() == null || turma.getId() == null || turma.getAno() == null) {
                continue;
            }

            String key = turma.getCurso().getId() + ":" + turma.getCurso().getTurno();
            turmasPorCursoTurno.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(turma);
        }

        List<GrupoSincronizacao> candidatos = new ArrayList<>();
        for (Map.Entry<String, List<Turma>> entry : turmasPorCursoTurno.entrySet()) {
            List<Turma> turmas = entry.getValue().stream()
                    .distinct()
                    .sorted(Comparator.comparing(Turma::getId))
                    .toList();

            for (int i = 0; i < turmas.size(); i++) {
                for (int j = i + 1; j < turmas.size(); j++) {
                    Turma turma1 = turmas.get(i);
                    Turma turma2 = turmas.get(j);
                    if (Objects.equals(turma1.getAno(), turma2.getAno())) {
                        continue;
                    }

                    List<AulaPlanejamento> blocosTurma1 = blocosPorCursoTurnoTurma.getOrDefault(
                            turma1.getCurso().getId() + ":" + turma1.getCurso().getTurno() + ":" + turma1.getId(),
                            List.of());
                    List<AulaPlanejamento> blocosTurma2 = blocosPorCursoTurnoTurma.getOrDefault(
                            turma2.getCurso().getId() + ":" + turma2.getCurso().getTurno() + ":" + turma2.getId(),
                            List.of());

                    int limitePeriodos = limiteSincronizacao(turma1, turma2);
                    if (limitePeriodos <= 0) {
                        continue;
                    }

                    Map<Integer, List<List<AulaPlanejamento>>> subconjuntos1 =
                            gerarSubconjuntosPorTotal(blocosTurma1, limitePeriodos);
                    Map<Integer, List<List<AulaPlanejamento>>> subconjuntos2 =
                            gerarSubconjuntosPorTotal(blocosTurma2, limitePeriodos);

                    for (Map.Entry<Integer, List<List<AulaPlanejamento>>> subconjunto : subconjuntos1.entrySet()) {
                        int totalPeriodos = subconjunto.getKey();
                        List<List<AulaPlanejamento>> alternativas2 = subconjuntos2.get(totalPeriodos);
                        if (alternativas2 == null) {
                            continue;
                        }
                        for (List<AulaPlanejamento> subset1 : subconjunto.getValue()) {
                            for (List<AulaPlanejamento> subset2 : alternativas2) {
                                Map<Integer, List<AulaPlanejamento>> blocosPorTurma = new LinkedHashMap<>();
                                blocosPorTurma.put(turma1.getId(), new ArrayList<>(subset1));
                                blocosPorTurma.put(turma2.getId(), new ArrayList<>(subset2));

                                candidatos.add(new GrupoSincronizacao(
                                        gerarIdGrupo(turma1, turma2, subset1, subset2),
                                        totalPeriodos,
                                        blocosPorTurma
                                ));
                            }
                        }
                    }
                }
            }
        }

        return escolherMelhoresGrupos(candidatos);
    }

    private List<GrupoSincronizacao> escolherMelhoresGrupos(
            List<GrupoSincronizacao> candidatos) {

        if (candidatos == null || candidatos.isEmpty()) {
            return List.of();
        }

        List<GrupoSincronizacao> ordenados
                = new ArrayList<>(candidatos);

        ordenados.sort(Comparator
                .comparingInt(GrupoSincronizacao::totalPeriodos)
                .reversed()
                .thenComparing(GrupoSincronizacao::id));

        Set<String> blocosUsados = new HashSet<>();
        List<GrupoSincronizacao> selecionados = new ArrayList<>();

        for (GrupoSincronizacao grupo : ordenados) {

            Set<String> chavesGrupo = new HashSet<>();

            for (List<AulaPlanejamento> blocos
                    : grupo.blocosPorTurma().values()) {

                for (AulaPlanejamento aula : blocos) {

                    String chave = aula.getId() != null
                            ? aula.getId()
                            : Integer.toHexString(
                                    System.identityHashCode(aula));

                    chavesGrupo.add(chave);
                }
            }

            boolean conflito = chavesGrupo.stream()
                    .anyMatch(blocosUsados::contains);

            if (!conflito) {
                selecionados.add(grupo);
                blocosUsados.addAll(chavesGrupo);
            }
        }

        return selecionados;
    }

    private int limiteSincronizacao(Turma turma1, Turma turma2) {
        int limite1 = turma1.getTamanhoGrade() == null ? Integer.MAX_VALUE : turma1.getTamanhoGrade().getQtdePeriodosDia();
        int limite2 = turma2.getTamanhoGrade() == null ? Integer.MAX_VALUE : turma2.getTamanhoGrade().getQtdePeriodosDia();
        return Math.min(limite1, limite2);
    }

    /**
    * Monta no máximo um subconjunto por total de períodos, sem enumerar os
    * 2^n subconjuntos possíveis. Totais acima do tamanho do turno não podem
    * formar um grupo sincronizável e são descartados durante a construção.
    */
    private Map<Integer, List<List<AulaPlanejamento>>> gerarSubconjuntosPorTotal(
            List<AulaPlanejamento> blocos,
            int limitePeriodos) {
        Map<Integer, List<List<AulaPlanejamento>>> subconjuntos = new LinkedHashMap<>();
        if (blocos == null || blocos.isEmpty()) {
            return subconjuntos;
        }

        subconjuntos.put(0, List.of(List.of()));
        for (AulaPlanejamento bloco : blocos) {
            int duracao = bloco.getDuracaoPeriodos();
            if (duracao <= 0 || duracao > limitePeriodos) {
                continue;
            }

            Map<Integer, List<List<AulaPlanejamento>>> novosSubconjuntos = new LinkedHashMap<>();
            subconjuntos.forEach((total, alternativas) ->
                    novosSubconjuntos.put(total, new ArrayList<>(alternativas)));
            for (Map.Entry<Integer, List<List<AulaPlanejamento>>> existente : subconjuntos.entrySet()) {
                int novoTotal = existente.getKey() + duracao;
                if (novoTotal > limitePeriodos) {
                    continue;
                }
                List<List<AulaPlanejamento>> alternativas = novosSubconjuntos.computeIfAbsent(
                        novoTotal, ignorado -> new ArrayList<>());
                for (List<AulaPlanejamento> alternativa : existente.getValue()) {
                    if (alternativas.size() >= MAX_SUBCONJUNTOS_POR_TOTAL) {
                        break;
                    }
                    List<AulaPlanejamento> novoSubconjunto = new ArrayList<>(alternativa);
                    novoSubconjunto.add(bloco);
                    alternativas.add(List.copyOf(novoSubconjunto));
                }
            }
            subconjuntos = novosSubconjuntos;
        }

        subconjuntos.remove(0);
        return subconjuntos;
    }

    private boolean ehTecnica(AulaPlanejamento aula) {

        if (aula == null || aula.getDisciplina() == null) {
            return false;
        }

        String tipo = aula.getDisciplina().getTipoDisciplina();

        return tipo != null
                && (tipo.equalsIgnoreCase("tecnica")
                || tipo.equalsIgnoreCase("técnica"));
    }

    private String gerarIdGrupo(
            Turma turma1,
            Turma turma2,
            List<AulaPlanejamento> subset1,
            List<AulaPlanejamento> subset2) {

        List<String> ids = new ArrayList<>();

        for (AulaPlanejamento aula : subset1) {
            ids.add(aula.getId());
        }

        for (AulaPlanejamento aula : subset2) {
            ids.add(aula.getId());
        }

        ids.sort(String::compareTo);

        return "grupo-" + turma1.getId()
                + "-" + turma2.getId()
                + "-" + String.join("-", ids);
    }
}
