package com.example.EduGrid.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.service.CursoService;

@Controller
@RequestMapping("/curso")
public class CursoController {
    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping({ "", "/", "/curso" })
    public String listarCursos(Model model) {
        List<Curso> cursos = cursoService.listarCursos();
        model.addAttribute("cursos", cursos);
        return "curso/curso";
    }

    @GetMapping("/salvar")
    public String abrirFormularioSalvar(Model model) {
        model.addAttribute("curso", new Curso());
        return "curso/salvarCurso";
    }

    @PostMapping("/salvar")
    public String salvarCurso(@ModelAttribute Curso curso) {
        cursoService.salvarCurso(curso);
        return "redirect:/curso/curso";
    }

    @GetMapping("/atualizar")
    public String abrirFormularioAtualizacao(@RequestParam(required = false) Integer id, Model model) {
        if (id == null) {
            return "redirect:/curso/curso";
        }

        Curso curso = cursoService.buscarCursoPorId(id);
        if (curso == null) {
            return "redirect:/curso/curso";
        }

        model.addAttribute("curso", curso);
        return "curso/atualizarCurso";
    }

    @PostMapping("/atualizar")
    public String atualizarCurso(@RequestParam Integer id, @ModelAttribute Curso curso) {
        cursoService.atualizarCurso(id, curso);
        return "redirect:/curso/curso";
    }

    @GetMapping("/deletar")
    public String abrirFormularioExclusao(@RequestParam(required = false) Integer id, Model model) {
        model.addAttribute("id", id);
        return "curso/deletarCurso";
    }

    @PostMapping("/deletar")
    public String deletarCurso(@RequestParam Integer id) {
        cursoService.excluirCurso(id);
        return "redirect:/curso/curso";
    }
}
