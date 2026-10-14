package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import java.util.List;
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
   
    public Turma(Integer id, String nome, List<Disciplina> disciplinas, Curso curso, TamanhoGrade tamanhoGrade) {
        this.id = id;
        this.nome = nome;
        this.disciplinas = disciplinas;
        this.curso = curso;
        this.tamanhoGrade = tamanhoGrade;
        if (tamanhoGrade != null) {
            setGradeHorariaMatriz(tamanhoGrade);
        }
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

}