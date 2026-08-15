package com.example.EduGrid.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.EduGrid.model.Professor;
import com.example.EduGrid.service.ProfessorService;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/professor")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }
    
    @GetMapping("/professor")
    public String abrirProfessor(Model model) {
        List<Professor> professores = professorService.listarProfessores();
        model.addAttribute("professores", professores);

        return "professor/professor";
    }

    // Abre o formulário de cadastro
    @GetMapping("/salvar")
    public String getAbrirFormularioSalvar(Model model) {

        model.addAttribute("professor", new Professor());

        return "professor/salvarProfessor";
    }

    // Recebe os dados do formulário e salva no banco
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Professor professor) {

        System.out.println("Entrou no método salvar");
        System.out.println(professor.getNome());

        professorService.salvarProfessor(professor);

        System.out.println("Professor salvo com sucesso!");

        return "redirect:/professor/professor";
    }
    
    @GetMapping("/deletar")
    public String getAbrirFormularioExclusao() {
        return "/professor/deletarProfessor";
    }
    
    @PostMapping("/deletar")
    public String deletarProfessor(@RequestParam Integer id) {
        professorService.excluirProfessor(id);

        return "redirect:/professor/professor";
    }

    @GetMapping("/atualizar")
    public String getAbrirFormularioAtualizacao() {
        return "/professor/atualizarProfessor";
    }

    @PostMapping("/atualizar")
    public String atualizarProfessor(@RequestParam Integer id, @ModelAttribute Professor professorAtualizado) {
        professorService.atualizarProfessor(id, professorAtualizado);
        return "redirect:/professor/professor";
    }
}