package com.example.EduGrid.service;

import java.util.List;

import com.example.EduGrid.model.Disciplina;
import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Turma;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.repository.TurmaRepository;

@Service
public class TurmaService {
    private final TurmaRepository turmaRepository;

    public TurmaService(TurmaRepository turmaRepository) {
        this.turmaRepository = turmaRepository;
    }

    public Turma salvarTurma(Turma turma) {
        return turmaRepository.save(turma);
    }
    
    public Turma BuscarTurmaPorId(Integer id) {
        return turmaRepository.findById(id).orElse(null);
    }

    public void excluirTurma(Integer id) {
        turmaRepository.deleteById(id);
    }

    public Turma atualizarTurma(Integer id, Turma turmaAtualizada) {
        Turma turmaExistente = turmaRepository.findById(id).orElse(null);
        if (turmaExistente != null) {
            turmaExistente.setNome(turmaAtualizada.getNome());
            turmaExistente.setCurso(turmaAtualizada.getCurso());
            turmaExistente.setDisciplinas(turmaAtualizada.getDisciplinas());
            return turmaRepository.save(turmaExistente);
        }
        return null;
    }

    public List<Turma> listarTurmas() {
        return turmaRepository.findAll();
    }

    public void alocarGradeHoraria(Turma turma, TamanhoGrade tamanhoGrade) {
        if (turma == null) {
            throw new IllegalArgumentException("A turma é obrigatória.");
        }
        if (tamanhoGrade == null) {
            throw new IllegalArgumentException("A configuração do tamanho da grade é obrigatória.");
        }

        turma.setTamanhoGrade(tamanhoGrade);
        turma.setGradeHorariaMatriz(tamanhoGrade);
        List<Disciplina> disciplinas = turma.getDisciplinas();
        if (disciplinas == null) {
            return;
        }

        for (Disciplina disciplina : disciplinas) {
            if (disciplina == null) {
                continue;
            }

            disciplina.setNumPeriodos(tamanhoGrade);
            int periodosNecessarios = disciplina.getNumPeriodos();
            int periodosAlocados = 0;

            for (int dia = 0; dia < tamanhoGrade.getQtdeDiasSemana()
                    && periodosAlocados < periodosNecessarios; dia++) {
                for (int periodo = 0; periodo < tamanhoGrade.getQtdePeriodosDia()
                        && periodosAlocados < periodosNecessarios; periodo++) {
                    if (turma.getGradeHorariaMatriz()[dia][periodo] == 0 && disciplina.getId() != null) {
                        turma.getGradeHorariaMatriz()[dia][periodo] = disciplina.getId();
                        periodosAlocados++;
                    }
                }
            }

            if (periodosAlocados < periodosNecessarios) {
                System.out.println("Não foi possível alocar completamente a disciplina: " + disciplina.getNome());
            }
        }
    }
}
