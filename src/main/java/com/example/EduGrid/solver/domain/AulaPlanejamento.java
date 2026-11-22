package com.example.EduGrid.solver.domain;

import java.util.List;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Turma;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;

@PlanningEntity
public class AulaPlanejamento {

    @PlanningId
    private String id;

    private Turma turma;
    private Disciplina disciplina;

    private int duracaoPeriodos;

    private List<Professor> professoresCandidatos = List.of();
    private List<HorarioPlanejamento> horariosCandidatos = List.of();

    @ValueRangeProvider(id = "professoresDaAula")
    public List<Professor> getProfessoresCandidatos() {
        return professoresCandidatos;
    }

    @ValueRangeProvider(id = "horariosDaAula")
    public List<HorarioPlanejamento> getHorariosCandidatos() {
        return horariosCandidatos;
    }

    public void setCandidatos(List<Professor> professores, List<HorarioPlanejamento> horarios) {
        professoresCandidatos = List.copyOf(professores);
        horariosCandidatos = List.copyOf(horarios);
    }

    @PlanningVariable(valueRangeProviderRefs = {"professoresDaAula"})
    private Professor professor;

    @PlanningVariable(valueRangeProviderRefs = {"horariosDaAula"})
    private HorarioPlanejamento horarioInicial;

    @PlanningPin
    private boolean bloqueadoManualmente;

    public AulaPlanejamento() {
    }

    public AulaPlanejamento(String id, Turma turma, Disciplina disciplina, int duracaoPeriodos, Professor professor, HorarioPlanejamento horarioInicial) {
        this.id = id;
        this.turma = turma;
        this.disciplina = disciplina;
        this.duracaoPeriodos = duracaoPeriodos;
        this.professor = professor;
        this.horarioInicial = horarioInicial;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Turma getTurma() {
        return turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(Disciplina disciplina) {
        this.disciplina = disciplina;
    }

    public int getDuracaoPeriodos() {
        return duracaoPeriodos;
    }

    public void setDuracaoPeriodos(int duracaoPeriodos) {
        this.duracaoPeriodos = duracaoPeriodos;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public HorarioPlanejamento getHorarioInicial() {
        return horarioInicial;
    }

    public void setHorarioInicial(HorarioPlanejamento horarioInicial) {
        this.horarioInicial = horarioInicial;
    }

    public boolean isBloqueadoManualmente() {
        return bloqueadoManualmente;
    }

    public void setBloqueadoManualmente(boolean bloqueadoManualmente) {
        this.bloqueadoManualmente = bloqueadoManualmente;
    }

}
