package com.example.EduGrid.service;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Professor;
import com.example.EduGrid.repository.ProfessorRepository;

import java.util.List;

@Service
public class ProfessorService {
    private final ProfessorRepository professorRepository;

    public ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    public Professor buscarProfessorPorId(Integer id) {
        return professorRepository.findById(id).orElse(null);
    }

    public List<Professor> listarProfessores() {
        return professorRepository.findAll();
    }
    
    public Professor salvarProfessor(Professor professor) {
        return professorRepository.save(professor) ;
    }

    public Professor atualizarProfessor(Integer id, Professor professorAtualizado) {
        Professor professorExistente = professorRepository.findById(id).orElse(null);
        if (professorExistente != null) {
            professorExistente.setNome(professorAtualizado.getNome());
            professorExistente.setAreaAtuacao(professorAtualizado.getAreaAtuacao());
            professorExistente.setCargaHoraria(professorAtualizado.getCargaHoraria());
            return professorRepository.save(professorExistente);
        }
        return null;
    }

    public void excluirProfessor(Integer id) {
        professorRepository.deleteById(id);
    }

    public Double calcularMediaCargaHoraria() {
        return professorRepository.calcularMediaCargaHoraria();
    }

    public List<Professor> professoresAcimaDaMedia() {
        return professorRepository.professoresAcimaDaMedia();
    }

    public List<Object[]> contarProfessoresPorAreaAtuacao() {
        return professorRepository.contarProfessoresPorAreaAtuacao();
    }

    public List<Professor> professorComMaiorCargaHoraria() {
        return professorRepository.professorComMaiorCargaHoraria();
    }
}
