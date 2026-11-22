package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;

@Entity
public class Disciplina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nome;
    private String areaAtuacao;
    private double cargaHoraria;
    @ManyToOne
    private TamanhoGrade TamanhoGrade;
    private int numPeriodos;
    @ManyToOne
    private Curso curso;
    private String tipoDisciplina; // técnica, comum, eletiva ou nivelamento
    private int MinPeriodosJuntos; // quantidade de sequencias de períodos que a disciplina deve ser alocada junto (ex: 2 períodos seguidos, 3 períodos seguidos, etc.)
    private int MaxPeriodosJuntos; // quantidade máxima de sequencias de períodos que a disciplina pode ser alocada junto (ex: 2 períodos seguidos, 3 períodos seguidos, etc.)

    public Disciplina(Integer id, String nome, String areaAtuacao,  double cargaHoraria, TamanhoGrade tamanhoGrade, Curso curso, String tipoDisciplina) {
        this.id = id;
        this.nome = nome;
        this.areaAtuacao = areaAtuacao;
        this.TamanhoGrade = tamanhoGrade;
        setCargaHoraria(cargaHoraria);
        this.curso = curso;
        this.tipoDisciplina = tipoDisciplina;
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
        recalcularNumPeriodos();
    }

    public void setTamanhoGrade(TamanhoGrade tamanhoGrade) {
        this.TamanhoGrade = tamanhoGrade;
        recalcularNumPeriodos();
    }

    public TamanhoGrade getTamanhoGrade() {
        return TamanhoGrade;
    }

    public String getTipoDisciplina() {
        return tipoDisciplina;
    }

    public String getAreaAtuacao() {
        return areaAtuacao;
    }

    public void setAreaAtuacao(String areaAtuacao) {
        this.areaAtuacao = areaAtuacao;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public void setTipoDisciplina(String tipoDisciplina) {
        this.tipoDisciplina = tipoDisciplina;
    }

    public int getNumPeriodos() {
        return numPeriodos;
    }

    public double converterHoraParaMinutos(double cargaHoraria) {
        return cargaHoraria * 60;
    }

    public void recalcularNumPeriodos() {
        double cargaHorariaMinutos = converterHoraParaMinutos(cargaHoraria);
        int tamanhoPeriodo = TamanhoGrade == null ? 45 : TamanhoGrade.getTamanhoPeriodo();
        if (tamanhoPeriodo <= 0) {
            throw new IllegalStateException("O tamanho do período deve ser maior que zero.");
        }
        this.numPeriodos = (int) Math.ceil(cargaHorariaMinutos / tamanhoPeriodo);
    }

    public int getMinPeriodosJuntos() {
        return MinPeriodosJuntos;
    }
    public void setMinPeriodosJuntos(int minPeriodosJuntos) {
        MinPeriodosJuntos = minPeriodosJuntos;
    }

    public int getMaxPeriodosJuntos() {
        return MaxPeriodosJuntos;
    }

    public void setMaxPeriodosJuntos(int maxPeriodosJuntos) {
        MaxPeriodosJuntos = maxPeriodosJuntos;
    }
}
