package com.example.EduGrid.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nome;
    @ManyToMany
    private List<Disciplina> disciplinas;
    @ManyToOne
    private Curso curso;
    @ManyToOne
    private TamanhoGrade tamanhoGrade;
    @Transient
    private int[][] gradeHorariaMatriz;
    @Column(nullable = false)
    private Integer ano;

    public Turma(Integer id, String nome, List<Disciplina> disciplinas, Curso curso, TamanhoGrade tamanhoGrade) {
        this(id, nome, disciplinas, curso, tamanhoGrade, null);
    }

    public Turma(Integer id, String nome, List<Disciplina> disciplinas, Curso curso, TamanhoGrade tamanhoGrade, Integer ano) {
        this.id = id;
        this.nome = nome;
        this.disciplinas = disciplinas;
        this.curso = curso;
        this.tamanhoGrade = tamanhoGrade;
        if (tamanhoGrade != null) {
            setGradeHorariaMatriz(tamanhoGrade);
        }
        this.ano = ano;
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

    public List<Disciplina> getDisciplinas() {
        return disciplinas;
    }

    public void setDisciplinas(List<Disciplina> disciplinas) {
        this.disciplinas = disciplinas;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public TamanhoGrade getTamanhoGrade() {
        return tamanhoGrade;
    }

    public void setTamanhoGrade(TamanhoGrade tamanhoGrade) {
        this.tamanhoGrade = tamanhoGrade;
    }

    public int[][] getGradeHorariaMatriz() {
        return gradeHorariaMatriz;
    }

    public void setGradeHorariaMatriz(TamanhoGrade tamanhoGrade) {
        this.gradeHorariaMatriz = tamanhoGrade == null ? null : new int[tamanhoGrade.getQtdeDiasSemana()][tamanhoGrade.getQtdePeriodosDia()];
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

}
