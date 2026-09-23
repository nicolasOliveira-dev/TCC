package com.example.EduGrid.model;

import java.util.List;

public class TurmaGrade {
    private Integer id;
    private String nome;
    private List<Disciplina> disciplinas;
    private String curso;
    private TamanhoGrade tamanhoGrade;
    private int[][] gradeHorariaMatriz;
   

    public TurmaGrade(Integer id, String nome, List<Disciplina> disciplinas, String curso, TamanhoGrade tamanhoGrade) {
        this.id = id;
        this.nome = nome;
        this.disciplinas = disciplinas;
        this.curso = curso;
        this.tamanhoGrade = tamanhoGrade;
        this.gradeHorariaMatriz = new int[tamanhoGrade.getQtdeDiasSemana()][tamanhoGrade.getQtdePeriodosDia()];
    }

    public TurmaGrade() {

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

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
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
        this.gradeHorariaMatriz = new int[tamanhoGrade.getQtdeDiasSemana()][tamanhoGrade.getQtdePeriodosDia()];
    }

    public void alocarGradeHoraria(List<Disciplina> disciplinas, TamanhoGrade tamanhoGrade) {

        setGradeHorariaMatriz(tamanhoGrade);

        int quantidadeDias = tamanhoGrade.getQtdeDiasSemana();
        int quantidadePeriodosDia = tamanhoGrade.getQtdePeriodosDia();

        for (Disciplina disciplina : disciplinas) {

                disciplina.setNumPeriodos(tamanhoGrade);

                int periodosNecessarios = disciplina.getNumPeriodos();

                int periodosAlocados = 0;

                for (int dia = 0; dia < quantidadeDias && periodosAlocados < periodosNecessarios;dia++) {

                    for (int periodo = 0;periodo < quantidadePeriodosDia && periodosAlocados < periodosNecessarios;periodo++) {

                        if (gradeHorariaMatriz[dia][periodo] == 0) {

                            gradeHorariaMatriz[dia][periodo] = disciplina.getId();

                            periodosAlocados++;
                        }
                   }
                }
            if (periodosAlocados < periodosNecessarios) {

                System.out.println( "Não foi possível alocar completamente a disciplina: " + disciplina.getNome());

            }
        }
    }
}

