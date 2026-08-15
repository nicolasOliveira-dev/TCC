package com.example.EduGrid.model;


public class Gerador {
    private int[][] tamanhoGrade;
    private GradeHoraria gradeHoraria;
    
    public Gerador(int[][] tamanhoGrade, GradeHoraria gradeHoraria) {
        this.tamanhoGrade = tamanhoGrade;
        this.gradeHoraria = gradeHoraria;
    }
     
    public Gerador() {
    }

    public void setGradeHoraria(GradeHoraria gradeHoraria) {
        this.gradeHoraria = gradeHoraria;
    }

    public GradeHoraria getGradeHoraria() {
        return gradeHoraria;
    }
    public int[][] getTamanhoGrade() {
        return tamanhoGrade;
    }

    public void setTamanhoGrade(GradeHoraria gradeHoraria) {
        this.tamanhoGrade = new int [gradeHoraria.getQtdeDiasSemana()] [gradeHoraria.getQtdePeriodosDia()];
    }
}

