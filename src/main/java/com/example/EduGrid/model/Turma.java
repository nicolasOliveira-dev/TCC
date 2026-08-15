package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Turma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nome;
    private String disciplina;
    private String curso;
    private String historicoProfessor;

    public Turma(Integer id, String nome, String disciplina, String curso, String historicoProfessor) {
        this.id = id;
        this.nome = nome;
        this.disciplina = disciplina;
        this.curso = curso;
        this.historicoProfessor = historicoProfessor;
    }

    public Turma() {

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

    public String getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(String disciplina) {
        this.disciplina = disciplina;
    }

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getHistoricoProfessor() {
        return historicoProfessor;
    }

    public void setHistoricoProfessor(String historicoProfessor) {
        this.historicoProfessor = historicoProfessor;
    }

}
