package com.example.EduGrid.solver.domain;

import java.util.ArrayList;
import java.util.List;

import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Turma;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;

@PlanningSolution
public class GradePlanejamento {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "professores")
    private List<Professor> professores;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "horarios")
    private List<HorarioPlanejamento> horariosPossiveis;

    @PlanningEntityCollectionProperty
    private List<AulaPlanejamento> aulas;

    @PlanningScore
    private HardSoftScore score;

    @ProblemFactCollectionProperty
    private List<GrupoSincronizacao> gruposSincronizacao;

    @ProblemFactCollectionProperty
    private List<VinculoSincronizacao> vinculosSincronizacao;

    @ProblemFactCollectionProperty
    private List<Turma> turmas;

    public GradePlanejamento() {
    }

    public GradePlanejamento(List<AulaPlanejamento> aulas, List<HorarioPlanejamento> horariosPossiveis, List<Professor> professores, HardSoftScore score, List<GrupoSincronizacao> gruposSincronizacao) {
        this(aulas, horariosPossiveis, professores, score, gruposSincronizacao, new ArrayList<>());
    }

    public GradePlanejamento(List<AulaPlanejamento> aulas, List<HorarioPlanejamento> horariosPossiveis, List<Professor> professores, HardSoftScore score, List<GrupoSincronizacao> gruposSincronizacao, List<VinculoSincronizacao> vinculosSincronizacao) {
        this.aulas = aulas;
        this.horariosPossiveis = horariosPossiveis;
        this.professores = professores;
        this.score = score;
        this.gruposSincronizacao = gruposSincronizacao == null ? new ArrayList<>() : gruposSincronizacao;
        this.vinculosSincronizacao = vinculosSincronizacao == null ? new ArrayList<>() : vinculosSincronizacao;
        this.turmas = new ArrayList<>();
        if (aulas != null && professores != null && horariosPossiveis != null) {
            aulas.forEach(aula -> aula.setCandidatos(professores, horariosPossiveis));
        }
    }

    public GradePlanejamento(List<AulaPlanejamento> aulas, List<HorarioPlanejamento> horariosPossiveis, List<Professor> professores) {
        this(aulas, horariosPossiveis, professores, null, new ArrayList<>(), new ArrayList<>());
    }

    public GradePlanejamento(List<AulaPlanejamento> aulas, List<HorarioPlanejamento> horariosPossiveis, List<Professor> professores, HardSoftScore score) {
        this(aulas, horariosPossiveis, professores, score, new ArrayList<>(), new ArrayList<>());
    }

    // Construtores, getters e setters

    public List<Professor> getProfessores() {
        return professores;
    }

    public void setProfessores(List<Professor> professores) {
        this.professores = professores;
    }

    public List<HorarioPlanejamento> getHorariosPossiveis() {
        return horariosPossiveis;
    }

    public void setHorariosPossiveis(List<HorarioPlanejamento> horariosPossiveis) {
        this.horariosPossiveis = horariosPossiveis;
    }

    public List<AulaPlanejamento> getAulas() {
        return aulas;
    }

    public void setAulas(List<AulaPlanejamento> aulas) {
        this.aulas = aulas;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public List<GrupoSincronizacao> getGruposSincronizacao() {
        return gruposSincronizacao;
    }

    public void setGruposSincronizacao(List<GrupoSincronizacao> gruposSincronizacao) {
        this.gruposSincronizacao = gruposSincronizacao;
    }

    public List<VinculoSincronizacao> getVinculosSincronizacao() {
        return vinculosSincronizacao;
    }

    public void setVinculosSincronizacao(List<VinculoSincronizacao> vinculosSincronizacao) {
        this.vinculosSincronizacao = vinculosSincronizacao;
    }

    public List<Turma> getTurmas() {
        return turmas;
    }

    public void setTurmas(List<Turma> turmas) {
        this.turmas = turmas == null ? new ArrayList<>() : turmas;
    }
}
