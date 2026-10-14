package com.example.EduGrid.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.service.GradeHorariaService;
import com.example.EduGrid.service.TamanhoGradeService;
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
    public String abrirGerador(Model model) {
        TamanhoGrade tamanhoGrade = tamanhoGradeService.buscarConfiguracao();
        if (tamanhoGrade == null) {
            model.addAttribute("tamanhoGrade", null);
            return "Grade/gerar";
        }

        GradeHoraria gradeHoraria = new GradeHoraria(turmaService.listarTurmas(), tamanhoGrade);
        return exibirGrade(gradeHoraria, model);
    }

    @GetMapping("/configurar")
    public String abrirFormularioConfiguracao(Model model) {
        model.addAttribute("tamanhoGrade", tamanhoGradeService.buscarConfiguracao());
        return "Grade/gerar";
    }

    @PostMapping("/gerar")
    public String gerarGrade(@ModelAttribute GradeHoraria gradeHoraria, Model model) {
        TamanhoGrade tamanhoInformado = gradeHoraria.getTamanhoGrade();
        TamanhoGrade configuracaoAtual = tamanhoGradeService.buscarConfiguracao();

        if (configuracaoAtual == null) {
            configuracaoAtual = tamanhoGradeService.salvar(tamanhoInformado);
        } else {
            configuracaoAtual = tamanhoGradeService.atualizar(configuracaoAtual.getId(), tamanhoInformado);
        }

        gradeHoraria.setTamanhoGrade(configuracaoAtual);
        gradeHoraria.setTurmas(turmaService.listarTurmas());
        gradeHorariaService.salvarGradeHoraria(gradeHoraria);

        return exibirGrade(gradeHoraria, model);
    }

    private String exibirGrade(GradeHoraria gradeHoraria, Model model) {
        if (gradeHoraria.getTurmas() != null) {
            for (var turma : gradeHoraria.getTurmas()) {
                if (turma != null) {
                    turmaService.alocarGradeHoraria(turma, gradeHoraria.getTamanhoGrade());
                }
            }
        }
        gradeHoraria.gerarMatriz();
        model.addAttribute("grade", gradeHoraria);
        return "Grade/grade";
    }
}