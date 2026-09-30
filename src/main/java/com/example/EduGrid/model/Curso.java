package com.example.EduGrid.model;


public class Curso {
    private String nomeCurso;
    private String tipoCurso;
    private String turno;
    
    public Curso(String nomeCurso, String tipoCurso, String turno) {
        this.nomeCurso = nomeCurso;
        this.tipoCurso = tipoCurso;
        this.turno = turno;
    }

    public Curso() {
    }

    public String getNomeCurso() {
        return nomeCurso;
    }

    public String getTipoCurso() {
        return tipoCurso;
    }

    public String getTurno() {
        return turno;
    }

    public void setNomeCurso(String nomeCurso) {
        this.nomeCurso = nomeCurso;
    }

    public void setTipoCurso(String tipoCurso) {
        this.tipoCurso = tipoCurso;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }
}
