package com.example.EduGrid.service;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.repository.GradeHorariaRepository;

@Service
public class GradeHorariaService {
    private final GradeHorariaRepository gradeHorariaRepository;

    public GradeHorariaService(GradeHorariaRepository gradeHorariaRepository) {
        this.gradeHorariaRepository = gradeHorariaRepository;
    }

    public GradeHoraria salvarGradeHoraria(GradeHoraria gradeHoraria) {
        return gradeHorariaRepository.save(gradeHoraria);
    }

    public GradeHoraria buscarGradeHorariaPorId(Integer id) {
        return gradeHorariaRepository.findById(id).orElse(null);
    }

    public GradeHoraria atualizarGradeHoraria(Integer id, GradeHoraria gradeHorariaAtualizada) {
        GradeHoraria gradeHorariaExistente = gradeHorariaRepository.findById(id).orElse(null);
        if (gradeHorariaExistente != null) {
            gradeHorariaExistente.setQtdeTurnos(gradeHorariaAtualizada.getQtdeTurnos());
            gradeHorariaExistente.setQtdePeriodosDia(gradeHorariaAtualizada.getQtdePeriodosDia());
            gradeHorariaExistente.setQtdeDiasSemana(gradeHorariaAtualizada.getQtdeDiasSemana());
            return gradeHorariaRepository.save(gradeHorariaExistente);
        }
        return null;
    }
 
    public void excluirGradeHoraria(Integer id) {
        gradeHorariaRepository.deleteById(id);
    }
}

