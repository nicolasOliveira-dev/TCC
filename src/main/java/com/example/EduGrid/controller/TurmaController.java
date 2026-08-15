package com.example.EduGrid.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.EduGrid.model.Turma;
import com.example.EduGrid.service.TurmaService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
@RequestMapping("/turma")
public class TurmaController {
    private final TurmaService serviceTurma;
    
    public TurmaController(TurmaService serviceTurma) {
        this.serviceTurma = serviceTurma;
    }
    
    @GetMapping("/turma")
    public String abrirTurma(Model model) {
         List<Turma> turmas = serviceTurma.listarTurmas();
        model.addAttribute("turmas", turmas);
        return "turma/turma";
    }
    

    @GetMapping("/salvar")
    public String getAbrirFormularioSalvar(Model model) {
        model.addAttribute("turma", new Turma());
        return "turma/salvarTurma";
    }
    

    @PostMapping("/salvar")
    public String salvarTurma(@ModelAttribute Turma turma) {
         serviceTurma.salvarTurma(turma);
         return "redirect:/turma/turma";

    }

    @GetMapping("/deletar")
    public String abrirFormularioExclusao() {
        return "/turma/deletarTurma";
    }

    @PostMapping("/deletar")
    public String deletarTurma(@ModelAttribute Turma turma) {
        serviceTurma.excluirTurma(turma.getId());
        return "redirect:/turma/turma";
    }

    @GetMapping("/atualizar")
    public String abrirFormularioAtualizacao() {
        return "/turma/atualizarTurma";
    }
    
    @PostMapping("/atualizar")
    public String atualizarTurma(@RequestParam Integer id, @ModelAttribute Turma turma) {
        serviceTurma.atualizarTurma(id, turma);
        return "redirect:/turma/turma";
    }

    @GetMapping("/listar")
    public String listarTurmas(Model model) {
        
        List<Turma> turmas = serviceTurma.listarTurmas();
        

        model.addAttribute("turmas", turmas);

        return "turma/turma";
    }
    
}
