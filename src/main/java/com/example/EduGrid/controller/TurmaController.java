package com.example.EduGrid.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeSemanalVisualizacao;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.service.CursoService;
import com.example.EduGrid.service.TamanhoGradeService;
import com.example.EduGrid.service.TurmaService;

@Controller
@RequestMapping("/turma")
public class TurmaController {
    private final TurmaService serviceTurma;
    private final DisciplinaController disciplinaController;
    private final CursoService cursoService;
    private final TamanhoGradeService tamanhoGradeService;

    @Autowired
    public TurmaController(
            TurmaService serviceTurma,
            DisciplinaController disciplinaController,
            CursoService cursoService,
            TamanhoGradeService tamanhoGradeService) {
        this.serviceTurma = serviceTurma;
        this.disciplinaController = disciplinaController;
        this.cursoService = cursoService;
        this.tamanhoGradeService = tamanhoGradeService;
    }

    public TurmaController(TurmaService serviceTurma, DisciplinaController disciplinaController,
            CursoService cursoService) {
        this(serviceTurma, disciplinaController, cursoService, null);
    }

    @GetMapping("/{id}/grade")
    public String visualizarGradeTurma(@PathVariable Integer id, Model model) {
        Turma turma = serviceTurma.BuscarTurmaPorId(id);
        if (turma == null) {
            return "redirect:/dashboard";
        }

        var horarios = serviceTurma.listarHorariosDaGradeAtual(id);
        TamanhoGrade tamanho = turma.getTamanhoGrade();
        if (tamanho == null && tamanhoGradeService != null) {
            tamanho = tamanhoGradeService.buscarConfiguracao();
        }
        model.addAttribute("titulo", "Grade horária da turma");
        model.addAttribute("nomeSelecionado", turma.getNome());
        int turno = GradeSemanalVisualizacao.indiceDoTurno(
                turma.getCurso() == null ? null : turma.getCurso().getTurno());
        boolean haHorarioNoTurno = false;
        if (turno >= 0) {
            for (var horario : horarios) {
                if (horario.getTurno() == turno) {
                    haHorarioNoTurno = true;
                    break;
                }
            }
        }
        if ((turno < 0 || !haHorarioNoTurno) && !horarios.isEmpty()) {
            turno = horarios.get(0).getTurno();
        }
        if (turno < 0) {
            turno = 0;
        }
        model.addAttribute("gradeSemanal", GradeSemanalVisualizacao.deTurno(horarios, tamanho, turno));
        model.addAttribute("temHorarios", !horarios.isEmpty());
        return "horario/grade-semanal";
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
        model.addAttribute("disciplinas", disciplinaController.listarDisciplinas());
        model.addAttribute("cursos", cursoService.listarCursos());
        return "turma/salvarTurma";
    }

    @PostMapping("/salvar")
    public String salvarTurma(@ModelAttribute Turma turma) {
        serviceTurma.salvarTurma(turma);
        return "redirect:/turma/turma";

    }

    @GetMapping("/deletar")
    public String abrirFormularioExclusao(@RequestParam(required = false) Integer id, Model model) {
        model.addAttribute("id", id);
        return "turma/deletarTurma";
    }

    @PostMapping("/deletar")
    public String deletarTurma(@ModelAttribute Turma turma) {
        serviceTurma.excluirTurma(turma.getId());
        return "redirect:/turma/turma";
    }

    @GetMapping("/atualizar")
    public String abrirFormularioAtualizacao(@RequestParam(required = false) Integer id, Model model) {
        Turma turma = id == null ? new Turma() : serviceTurma.BuscarTurmaPorId(id);
        if (turma == null) {
            return "redirect:/turma/turma";
        }

        model.addAttribute("turma", turma);
        List<Curso> cursos = cursoService.listarCursos();
        model.addAttribute("cursos", cursos);
        model.addAttribute("tamanhoGrades", turma.getTamanhoGrade() == null ? List.of() : List.of(turma.getTamanhoGrade()));
        List<Disciplina> disciplinas = disciplinaController.listarDisciplinas();
        model.addAttribute("disciplinas", disciplinas);
        return "turma/atualizarTurma";
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
