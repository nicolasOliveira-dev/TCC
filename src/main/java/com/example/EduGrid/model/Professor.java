package com.example.EduGrid.model;

import java.util.List;

import com.example.EduGrid.model.converter.BooleanMatrixConverter;
import com.example.EduGrid.model.converter.StringListConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nome;
    private String areaAtuacao;
    private int cargaHoraria;
    @Convert(converter = StringListConverter.class)
    @Column(length = 255)
    private List<String> intencaoDiaPlanejamento;
    @Convert(converter = BooleanMatrixConverter.class)
    @Column(length = 512)
    private Boolean[][][] disponibilidadeHoraria;
    @ManyToMany
    private List<Disciplina> disciplinasPossiveis;
    private final int jornadaMaxima = 8;

    public Professor(Integer id, String nome, String areaAtuacao, int cargaHoraria, List<String> intencaoDiaPlanejamento, Boolean[][][] disponibilidadeHoraria, List<Disciplina> disciplinasPossiveis) {
        this.id = id;
        this.nome = nome;
        this.areaAtuacao = areaAtuacao;
        this.cargaHoraria = cargaHoraria;
        this.intencaoDiaPlanejamento = intencaoDiaPlanejamento;
        this.disponibilidadeHoraria = disponibilidadeHoraria;
        this.disciplinasPossiveis = disciplinasPossiveis;
    }

    public Professor() {

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

    public String getAreaAtuacao() {
        return areaAtuacao;
    }

    public void setAreaAtuacao(String areaAtuacao) {
        this.areaAtuacao = areaAtuacao;
    }

    public int getCargaHoraria() {
        return cargaHoraria;
    }

    public void setCargaHoraria(int cargaHoraria) {
        this.cargaHoraria = cargaHoraria;
    }

    public List<String> getIntencaoDiaPlanejamento() {
        return intencaoDiaPlanejamento;
    }

    public void setIntencaoDiaPlanejamento(List<String> intencaoDiaPlanejamento) {
        this.intencaoDiaPlanejamento = intencaoDiaPlanejamento;
    }

    public Boolean[][][] getDisponibilidadeHoraria() {
        return disponibilidadeHoraria;
    }

    public void setDisponibilidadeHoraria(Boolean[][][] disponibilidadeHoraria) {
        this.disponibilidadeHoraria = disponibilidadeHoraria;
    }

    public List<Disciplina> getDisciplinasPossiveis() {
        return disciplinasPossiveis;
    }

    public void setDisciplinasPossiveis(List<Disciplina> disciplinasPossiveis) {
        this.disciplinasPossiveis = disciplinasPossiveis;
    }

    public int getJornadaMaxima() {
        return jornadaMaxima;
    }
}
