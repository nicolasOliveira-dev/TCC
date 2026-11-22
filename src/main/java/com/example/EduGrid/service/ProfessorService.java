package com.example.EduGrid.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.ProfessorRepository;

@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final HorarioRepository horarioRepository;
    private final GradeHorariaService gradeHorariaService;

    @Autowired
    public ProfessorService(ProfessorRepository professorRepository, HorarioRepository horarioRepository,
            GradeHorariaService gradeHorariaService) {
        this.professorRepository = professorRepository;
        this.horarioRepository = horarioRepository;
        this.gradeHorariaService = gradeHorariaService;
    }

    /** Mantido para instanciação simples em código legado e testes. */
    public ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
        this.horarioRepository = null;
        this.gradeHorariaService = null;

    }

    public Professor buscarProfessorPorId(Integer id) {
        return professorRepository.findById(id).orElse(null);
    }

    public List<Professor> listarProfessores() {
        return professorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Horario> listarHorariosDaGradeAtual(Integer professorId) {
        Professor professor = professorRepository.findById(professorId).orElse(null);
        if (professor == null) {
            return List.of();
        }
        var grade = gradeHorariaService.buscarGradeMaisRecente();
        return grade == null
                ? horarioRepository.findByProfessorAndGradeHorariaIsNull(professor)
                : horarioRepository.findByProfessorAndGradeHoraria(professor, grade);
    }

    public Professor salvarProfessor(Professor professor) {
        return professorRepository.save(professor);
    }

    public Professor atualizarProfessor(Integer id, Professor professorAtualizado) {
        Professor professorExistente = professorRepository.findById(id).orElse(null);
        if (professorExistente != null) {
            professorExistente.setNome(professorAtualizado.getNome());
            professorExistente.setAreaAtuacao(professorAtualizado.getAreaAtuacao());
            professorExistente.setCargaHoraria(professorAtualizado.getCargaHoraria());
            professorExistente.setIntencaoDiaPlanejamento(professorAtualizado.getIntencaoDiaPlanejamento());
            professorExistente.setDisponibilidadeHoraria(professorAtualizado.getDisponibilidadeHoraria());
            professorExistente.setDisciplinasPossiveis(professorAtualizado.getDisciplinasPossiveis());
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
