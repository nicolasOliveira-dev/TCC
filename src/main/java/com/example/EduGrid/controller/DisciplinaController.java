package com.example.EduGrid.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.service.DisciplinaService;

@Controller
@RequestMapping("/disciplina")
public class DisciplinaController {
    private final DisciplinaService disciplinaService;
    
    @GetMapping("/disciplina")
    public String abrirDisciplina(Model model) {
        
        List<Disciplina> disciplinas = disciplinaService.listarDisciplinas();

        model.addAttribute("disciplinas", disciplinas);

        return "disciplina/disciplina";
    }
    

    public DisciplinaController(DisciplinaService disciplinaService) {
        this.disciplinaService = disciplinaService;
    }

    @GetMapping("/salvar")
    public String getAbrirFormularioSalvar(Model model) {
        model.addAttribute("disciplina", new Disciplina());
        return "/disciplina/salvarDisciplina";
    }

    @PostMapping("/salvar")
    public String salvarDisciplina(@ModelAttribute Disciplina disciplina) {
        disciplinaService.salvarDisciplina(disciplina);

        return "redirect:/disciplina/disciplina";
    }

    @GetMapping("/deletar")
    public String abrirFormularioExclusao() {
        return "disciplina/deletarDisciplina";
    }

    @PostMapping("/deletar")
    public String deletarDisciplina(@RequestParam Integer id) {
        disciplinaService.excluirDisciplina(id);

        return "redirect:/disciplina/disciplina";
    }

    @GetMapping("/atualizar")
    public String abrirFormularioAtualizacao() {
        return "/disciplina/atualizarDisciplina";
    }

    @PostMapping("/atualizar")
    public String atualizarDisciplina(@RequestParam Integer id, @ModelAttribute Disciplina disciplina) {
        disciplinaService.atualizarDisciplina(id, disciplina);
        
        return "redirect:/disciplina/disciplina";
    }
}
