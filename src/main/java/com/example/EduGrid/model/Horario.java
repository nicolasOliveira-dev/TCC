package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Horario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private int turno;
    private int periodoDia;
    private int diaSemana;
    private boolean manualmenteEditado;
    @ManyToOne
    private Turma turma;
    @ManyToOne
    private Disciplina disciplina;
    @ManyToOne(optional = true)
    private Professor professor;

    @ManyToOne
    private GradeHoraria gradeHoraria;

    public Horario(Integer id, int turno, int periodoDia, int diaSemana, Turma turma, Disciplina disciplina, Professor professor) {
        this.id = id;
        this.turno = turno;
        this.periodoDia = periodoDia;
        this.diaSemana = diaSemana;
        this.turma = turma;
        this.disciplina = disciplina;
        this.professor = professor;
    }
    public Horario() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getTurno() {
        return turno;
    }

    public void setTurno(int turno) {
        this.turno = turno;
    }

    public int getPeriodoDia() {
        return periodoDia;
    }

    public void setPeriodoDia(int periodoDia) {
        this.periodoDia = periodoDia;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public boolean isManualmenteEditado() {
        return manualmenteEditado;
    }

    public void setManualmenteEditado(boolean manualmenteEditado) {
        this.manualmenteEditado = manualmenteEditado;
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

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public GradeHoraria getGradeHoraria() {
        return gradeHoraria;
    }

    public void setGradeHoraria(GradeHoraria gradeHoraria) {
        this.gradeHoraria = gradeHoraria;
    }
}
