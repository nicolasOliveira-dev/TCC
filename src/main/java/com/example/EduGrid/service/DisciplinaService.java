package com.example.EduGrid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.repository.DisciplinaRepository;

@Service
public class DisciplinaService {
    private final DisciplinaRepository disciplinaRepository;

    public DisciplinaService(DisciplinaRepository disciplinaRepository) {
        this.disciplinaRepository = disciplinaRepository;
    }

    public Disciplina salvarDisciplina(Disciplina disciplina) {
        disciplina.recalcularNumPeriodos();
        return disciplinaRepository.save(disciplina);
    }

    public List<Disciplina> listarDisciplinas() {
        return disciplinaRepository.findAll();

    }

    public Disciplina buscarDisciplinaPorId(Integer id) {
        return disciplinaRepository.findById(id).orElse(null);
    }

    public Disciplina atualizarDisciplina(Integer id, Disciplina disciplinaAtualizada) {
        Disciplina disciplinaExistente = disciplinaRepository.findById(id).orElse(null);
        if (disciplinaExistente != null) {
            disciplinaExistente.setNome(disciplinaAtualizada.getNome());
            disciplinaExistente.setAreaAtuacao(disciplinaAtualizada.getAreaAtuacao());
            disciplinaExistente.setCargaHoraria(disciplinaAtualizada.getCargaHoraria());
            if (disciplinaAtualizada.getTamanhoGrade() != null) {
                disciplinaExistente.setTamanhoGrade(disciplinaAtualizada.getTamanhoGrade());
            }
            if (disciplinaAtualizada.getCurso() != null) {
                disciplinaExistente.setCurso(disciplinaAtualizada.getCurso());
            }
            disciplinaExistente.setTipoDisciplina(disciplinaAtualizada.getTipoDisciplina());
            disciplinaExistente.setMinPeriodosJuntos(disciplinaAtualizada.getMinPeriodosJuntos());
            disciplinaExistente.setMaxPeriodosJuntos(disciplinaAtualizada.getMaxPeriodosJuntos());

            return disciplinaRepository.save(disciplinaExistente);
        }
        return null;
    }

    public void excluirDisciplina(Integer id) {
        disciplinaRepository.deleteById(id);
    }

}
