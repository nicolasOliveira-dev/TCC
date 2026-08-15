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
            disciplinaExistente.setHistoricoProfessor(disciplinaAtualizada.getHistoricoProfessor());
            disciplinaExistente.setHistoricoTurmas(disciplinaAtualizada.getHistoricoTurmas());
            disciplinaExistente.setCargaHoraria(disciplinaAtualizada.getCargaHoraria());
            return disciplinaRepository.save(disciplinaExistente);
        }
        return null;
    }

    public void excluirDisciplina(Integer id) {
        disciplinaRepository.deleteById(id);
    }

    public List<Disciplina> disciplinasAcimaDaMedia() {
        return disciplinaRepository.disciplinasAcimaDaMedia();
    }

    public Integer calcularCargaHorariaTotal() {
        return disciplinaRepository.calcularCargaHorariaTotal();
    }

    public List<Object[]> disciplinasPorProfessor() {
        return disciplinaRepository.disciplinasPorProfessor();
    }

    public List<Disciplina> disciplinaComMaiorCargaHoraria() {
        return disciplinaRepository.disciplinaComMaiorCargaHoraria();
    }
    
}
