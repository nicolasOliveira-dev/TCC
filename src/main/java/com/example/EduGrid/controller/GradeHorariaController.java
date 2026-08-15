package com.example.EduGrid.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.EduGrid.model.Gerador;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.service.GradeHorariaService;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/gradeHoraria")
public class GradeHorariaController {

    private final GradeHorariaService gradeHorariaService;

    public GradeHorariaController(
            GradeHorariaService gradeHorariaService) {

        this.gradeHorariaService = gradeHorariaService;
    }

    @GetMapping("/gerar")
    public String abrirFormularioGerar() {

        return "Grade/gerar";
    }

    @PostMapping("/gerar")
    public String gerarGrade(
            @ModelAttribute GradeHoraria gradeHoraria,
            Model model) {

        gradeHorariaService.salvarGradeHoraria(gradeHoraria);

        Gerador gerador = new Gerador();

        gerador.setTamanhoGrade(gradeHoraria);

        model.addAttribute("grade", gerador);

        return "Grade/grade";
    }
    
}