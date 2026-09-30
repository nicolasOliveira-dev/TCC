package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.util.List;

import jakarta.persistence.Transient;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
public class GradeHoraria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToMany
    private List<Turma> turmas;

    @ManyToOne
    private TamanhoGrade tamanhoGrade;
    @Transient
    private Object[][] matriz;


    public GradeHoraria(List<Turma> turmas, TamanhoGrade tamanhoGrade) {
        this.turmas = turmas;
        this.tamanhoGrade = tamanhoGrade;
    }

    public GradeHoraria() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Turma> getTurmas() {
        return turmas;
    }

    public void setTurmas(List<Turma> turmas) {
        this.turmas = turmas;
    }

    public Object[][] getMatriz() {
        return matriz;
    }

    public void setMatriz(Object[][] matriz) {
        this.matriz = matriz;
    }

    public TamanhoGrade getTamanhoGrade() {
        return tamanhoGrade;
    }

    public void setTamanhoGrade(TamanhoGrade tamanhoGrade) {
        this.tamanhoGrade = tamanhoGrade;
    }

    public void gerarMatriz() {
        int quantidadeTurmas = turmas == null ? 0 : turmas.size();
        int totalPeriodos = tamanhoGrade.getQtdeDiasSemana()
                * tamanhoGrade.getQtdePeriodosDia();

        matriz = new Object[quantidadeTurmas][totalPeriodos];

    }
    
}

