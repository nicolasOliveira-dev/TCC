package com.example.EduGrid.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.persistence.Transient;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Column;

import java.util.HashMap;
import java.util.Map;

@Entity
public class GradeHoraria {
    private static final DateTimeFormatter FORMATO_DATA_GERACAO
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToMany
    private List<Turma> turmas;

    @ManyToOne
    private TamanhoGrade tamanhoGrade;

    private LocalDateTime dataGeracao;

    @Column(columnDefinition = "TEXT")
    private String relatorioRestricoesHard;

    @Column(columnDefinition = "TEXT")
    private String relatorioEdicao;

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

    public LocalDateTime getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(LocalDateTime dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public String getRelatorioRestricoesHard() {
        return relatorioRestricoesHard;
    }

    public void setRelatorioRestricoesHard(String relatorioRestricoesHard) {
        this.relatorioRestricoesHard = relatorioRestricoesHard;
    }

    public String getRelatorioEdicao() {
        return relatorioEdicao;
    }

    public void setRelatorioEdicao(String relatorioEdicao) {
        this.relatorioEdicao = relatorioEdicao;
    }

    public String getDataGeracaoFormatada() {
        return dataGeracao == null ? "" : dataGeracao.format(FORMATO_DATA_GERACAO);
    }

    @PrePersist
    private void registrarDataGeracao() {
        if (dataGeracao == null) {
            dataGeracao = LocalDateTime.now();
        }
    }

    public void gerarMatriz() {
        int quantidadeTurmas = turmas == null ? 0 : turmas.size();
        int quantidadeDias = tamanhoGrade.getQtdeDiasSemana();
        int periodosPorDia = tamanhoGrade.getQtdePeriodosDia();
        int totalPeriodos = quantidadeDias * periodosPorDia;

        matriz = new Object[quantidadeTurmas][totalPeriodos];

        for (int indiceTurma = 0; indiceTurma < quantidadeTurmas; indiceTurma++) {
            Turma turma = turmas.get(indiceTurma);
            if (turma == null) {
                continue;
            }

            int[][] matrizDaTurma = turma.getGradeHorariaMatriz();
            Map<Integer, String> nomesDisciplinas = new HashMap<>();

            if (turma.getDisciplinas() != null) {
                for (Disciplina disciplina : turma.getDisciplinas()) {
                    if (disciplina != null && disciplina.getId() != null) {
                        nomesDisciplinas.put(disciplina.getId(), disciplina.getNome());
                    }
                }
            }

            for (int dia = 0; dia < quantidadeDias; dia++) {
                for (int periodo = 0; periodo < periodosPorDia; periodo++) {
                    int idDisciplina = matrizDaTurma[dia][periodo];
                    if (idDisciplina != 0) {
                        matriz[indiceTurma][dia * periodosPorDia + periodo] = nomesDisciplinas.get(idDisciplina);
                    }
                }
            }
        }
    }

}
