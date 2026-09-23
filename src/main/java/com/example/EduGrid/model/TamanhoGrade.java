package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class TamanhoGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int qtdeTurnos;
    private int qtdePeriodosDia;
    private int qtdeDiasSemana;
    private int tamanhoPeriodo;

    public TamanhoGrade(int qtdeTurnos, int qtdePeriodosDia, int qtdeDiasSemana) {
        this.qtdeTurnos = qtdeTurnos;
        this.qtdePeriodosDia = qtdePeriodosDia;
        this.qtdeDiasSemana = qtdeDiasSemana;
        this.tamanhoPeriodo = 45; // Valor padrão, pode ser ajustado conforme necessário
        }
    public TamanhoGrade() {

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
    
    public int getTamanhoPeriodo() {
        return tamanhoPeriodo;
    }

    public void setTamanhoPeriodo(int tamanhoPeriodo) {
        this.tamanhoPeriodo = tamanhoPeriodo;
    }
}

