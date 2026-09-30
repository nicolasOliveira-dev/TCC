package com.example.EduGrid.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.repository.TamanhoGradeRepository;
@Service
public class GradeHorariaService {

    private final GradeHorariaRepository gradeHorariaRepository;
    private final TamanhoGradeRepository tamanhoGradeRepository;

    public GradeHorariaService(
            GradeHorariaRepository gradeHorariaRepository,
            TamanhoGradeRepository tamanhoGradeRepository) {

        this.gradeHorariaRepository = gradeHorariaRepository;
        this.tamanhoGradeRepository = tamanhoGradeRepository;
    }

    @Transactional
    public void salvarGradeHoraria(GradeHoraria gradeHoraria) {

        tamanhoGradeRepository.save(gradeHoraria.getTamanhoGrade());

        gradeHorariaRepository.save(gradeHoraria);
    }
}