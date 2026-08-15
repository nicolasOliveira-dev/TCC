package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class GradeHoraria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int qtdeTurnos;
    private int qtdePeriodosDia;
    private int qtdeDiasSemana;

   
    public GradeHoraria(int qtdeTurnos, int qtdePeriodosDia, int qtdeDiasSemana) {
        this.qtdeTurnos = qtdeTurnos;
        this.qtdePeriodosDia = qtdePeriodosDia;
        this.qtdeDiasSemana = qtdeDiasSemana;
       }
    public GradeHoraria() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getQtdeTurnos() {
        return qtdeTurnos;
    }

    public void setQtdeTurnos(int qtdeTurnos) {
        this.qtdeTurnos = qtdeTurnos;
    }

    public int getQtdePeriodosDia() {
        return qtdePeriodosDia;
    }

    public void setQtdePeriodosDia(int qtdePeriodosDia) {
        this.qtdePeriodosDia = qtdePeriodosDia;
    }

    public int getQtdeDiasSemana() {
        return qtdeDiasSemana;
    }

    public void setQtdeDiasSemana(int qtdeDiasSemana) {
        this.qtdeDiasSemana = qtdeDiasSemana;
    }
    
}

