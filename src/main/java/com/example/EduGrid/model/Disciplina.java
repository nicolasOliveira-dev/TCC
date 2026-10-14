package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class Disciplina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nome;
    private double cargaHoraria;
    private int numPeriodos;
    private String historicoProfessor;
    private String historicoTurmas;
    

    public Disciplina(Integer id, String nome, double cargaHoraria, int numPeriodos, String historicoProfessor, String historicoTurmas) {
        this.id = id;
        this.nome = nome;
        this.cargaHoraria = cargaHoraria;
        this.numPeriodos = numPeriodos;
        this.historicoProfessor = historicoProfessor;
        this.historicoTurmas = historicoTurmas;
    }
    public Disciplina() {
       
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getCargaHoraria() {
        return cargaHoraria;
    }

    public void setCargaHoraria(double cargaHoraria) {
        this.cargaHoraria = cargaHoraria;
    }

    public String getHistoricoProfessor() {
        return historicoProfessor;
    }

    public void setHistoricoProfessor(String historicoProfessor) {
        this.historicoProfessor = historicoProfessor;
    }

    public String getHistoricoTurmas() {
        return historicoTurmas;
    }

    public void setHistoricoTurmas(String historicoTurmas) {
        this.historicoTurmas = historicoTurmas;
    }

    public int getNumPeriodos() {
        return numPeriodos;
    }

    public double converterHoraParaMinutos(double cargaHoraria) {
        return cargaHoraria * 60;
    }
    
    public void setNumPeriodos(TamanhoGrade tamanhoGrade) {
        double cargaHorariaMinutos = converterHoraParaMinutos(cargaHoraria);
        this.numPeriodos = (int) Math.ceil(cargaHorariaMinutos / tamanhoGrade.getTamanhoPeriodo());
    }
}
