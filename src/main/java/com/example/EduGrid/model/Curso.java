package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
