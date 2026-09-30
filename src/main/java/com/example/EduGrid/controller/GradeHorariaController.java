package com.example.EduGrid.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.service.TamanhoGradeService;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.service.GradeHorariaService;
import com.example.EduGrid.service.TurmaService;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/gradeHoraria")
public class GradeHorariaController {

    private final GradeHorariaService gradeHorariaService;
    private final TamanhoGradeService tamanhoGradeService;
    private final TurmaService turmaService;

    public GradeHorariaController(
            GradeHorariaService gradeHorariaService,
            TamanhoGradeService tamanhoGradeService,
            TurmaService turmaService) {

        this.gradeHorariaService = gradeHorariaService;
        this.tamanhoGradeService = tamanhoGradeService;
        this.turmaService = turmaService;
    }

    @GetMapping("/gerar")
    public String abrirFormularioGerar() {

        return "Grade/gerar";
    }

   @PostMapping("/gerar")
    public String gerarGrade(@ModelAttribute GradeHoraria gradeHoraria, Model model) {

        gradeHoraria.setTurmas(turmaService.listarTurmas());
        gradeHorariaService.salvarGradeHoraria(gradeHoraria);

        gradeHoraria.gerarMatriz();

        model.addAttribute("grade", gradeHoraria);

        return "Grade/grade";
    }
    
}