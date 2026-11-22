package com.example.EduGrid.controller;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeSemanalVisualizacao;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.service.DisciplinaService;
import com.example.EduGrid.service.ProfessorService;
import com.example.EduGrid.service.TamanhoGradeService;

@Controller
@RequestMapping("/professor")
public class ProfessorController {

    private final ProfessorService professorService;
    private final TamanhoGradeService tamanhoGradeService;
    private final DisciplinaService disciplinaService;

    private static final List<String> DIAS_DA_SEMANA = List.of(
            "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
            "Sexta-feira", "Sábado", "Domingo");
    private static final List<String> NOMES_TURNOS = List.of("Manhã", "Tarde", "Noite");
    private static final TamanhoGrade CONFIGURACAO_PADRAO = new TamanhoGrade(2, 5, 5);

    public ProfessorController(
            ProfessorService professorService,
            TamanhoGradeService tamanhoGradeService,
            DisciplinaService disciplinaService) {
        this.professorService = professorService;
        this.tamanhoGradeService = tamanhoGradeService;
        this.disciplinaService = disciplinaService;
    }

    @GetMapping("/professor")
    public String abrirProfessor(Model model) {
        List<Professor> professores = professorService.listarProfessores();
        model.addAttribute("professores", professores);

        return "professor/professor";
    }

    @GetMapping("/{id}/grade")
    public String visualizarGradeProfessor(@PathVariable Integer id, Model model) {
        Professor professor = professorService.buscarProfessorPorId(id);
        if (professor == null) {
            return "redirect:/dashboard";
        }
        var horarios = professorService.listarHorariosDaGradeAtual(id);
        model.addAttribute("titulo", "Grade horária do professor");
        model.addAttribute("nomeSelecionado", professor.getNome());
        model.addAttribute("gradeSemanal", GradeSemanalVisualizacao.de(horarios, obterConfiguracaoGrade()));
        model.addAttribute("temHorarios", !horarios.isEmpty());
        return "horario/grade-semanal";
    }

    @GetMapping("/salvar")
    public String getAbrirFormularioSalvar(Model model) {
        Professor professor = new Professor();
        prepararFormulario(professor, model);
        model.addAttribute("professor", professor);
        return "professor/salvarProfessor";
    }

    @PostMapping("/salvar")
    public String salvar(
            @ModelAttribute Professor professor,
            @RequestParam(name = "horariosIndisponiveis", required = false) List<String> horariosIndisponiveis,
            @RequestParam(name = "disciplinasSelecionadas", required = false) List<Integer> disciplinasSelecionadas) {
        System.out.println("Entrou no método salvar");
        System.out.println(professor.getNome());
        professor.setDisciplinasPossiveis(resolverDisciplinas(disciplinasSelecionadas, professor.getAreaAtuacao()));
        configurarDisponibilidade(professor, horariosIndisponiveis, obterConfiguracaoGrade());
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
    public String getAbrirFormularioAtualizacao(@RequestParam(required = false) Integer id, Model model) {
        Professor professor = id == null ? null : professorService.buscarProfessorPorId(id);
        if (professor == null) {
            return "redirect:/professor/professor";
        }

        prepararFormulario(professor, model);
        model.addAttribute("professor", professor);
        return "professor/atualizarProfessor";
    }

    @PostMapping("/atualizar")
    public String atualizarProfessor(
            @RequestParam Integer id,
            @ModelAttribute Professor professorAtualizado,
            @RequestParam(name = "horariosIndisponiveis", required = false) List<String> horariosIndisponiveis,
            @RequestParam(name = "disciplinasSelecionadas", required = false) List<Integer> disciplinasSelecionadas) {
        professorAtualizado.setDisciplinasPossiveis(
                resolverDisciplinas(disciplinasSelecionadas, professorAtualizado.getAreaAtuacao()));
        configurarDisponibilidade(professorAtualizado, horariosIndisponiveis, obterConfiguracaoGrade());
        professorService.atualizarProfessor(id, professorAtualizado);
        return "redirect:/professor/professor";
    }

    private void prepararFormulario(Professor professor, Model model) {
        TamanhoGrade configuracao = obterConfiguracaoGrade();
        if (professor.getIntencaoDiaPlanejamento() == null) {
            professor.setIntencaoDiaPlanejamento(new ArrayList<>());
        }
        professor.setDisponibilidadeHoraria(ajustarDisponibilidade(
                professor.getDisponibilidadeHoraria(),
                configuracao.getQtdeTurnos(),
                configuracao.getQtdePeriodosDia(),
                configuracao.getQtdeDiasSemana()));

        List<String> dias = IntStream.range(0, configuracao.getQtdeDiasSemana())
                .mapToObj(indice -> indice < DIAS_DA_SEMANA.size()
                        ? DIAS_DA_SEMANA.get(indice)
                        : "Dia " + (indice + 1))
                .toList();
        List<String> turnos = IntStream.range(0, configuracao.getQtdeTurnos())
                .mapToObj(indice -> indice < NOMES_TURNOS.size()
                        ? NOMES_TURNOS.get(indice)
                        : "Turno " + (indice + 1))
                .toList();

        model.addAttribute("diasSemana", dias);
        model.addAttribute("nomesTurnos", turnos);
        model.addAttribute("quantidadePeriodos", configuracao.getQtdePeriodosDia());
        model.addAttribute("disciplinas", disciplinaService.listarDisciplinas());
        model.addAttribute("disciplinasSelecionadas", professor.getDisciplinasPossiveis() == null
                ? List.of()
                : professor.getDisciplinasPossiveis().stream()
                        .map(Disciplina::getId)
                        .filter(id -> id != null)
                        .toList());
    }

    private List<Disciplina> resolverDisciplinas(List<Integer> ids, String areaAtuacao) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Integer> idsUnicos = ids.stream().distinct().toList();
        List<Disciplina> disciplinas = disciplinaService.listarDisciplinas().stream()
                .filter(disciplina -> idsUnicos.contains(disciplina.getId()))
                .filter(disciplina -> normalizarArea(disciplina.getAreaAtuacao())
                        .equals(normalizarArea(areaAtuacao)))
                .toList();
        if (disciplinas.size() != idsUnicos.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Uma ou mais disciplinas selecionadas não correspondem à área de atuação do professor.");
        }
        return disciplinas;
    }

    private String normalizarArea(String areaAtuacao) {
        if (areaAtuacao == null) {
            return "";
        }
        return Normalizer.normalize(areaAtuacao, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private TamanhoGrade obterConfiguracaoGrade() {
        TamanhoGrade configuracao = tamanhoGradeService.buscarConfiguracao();
        if (configuracao == null) {
            return CONFIGURACAO_PADRAO;
        }
        if (configuracao.getQtdeTurnos() < 1
                || configuracao.getQtdePeriodosDia() < 1
                || configuracao.getQtdeDiasSemana() < 1) {
            throw new IllegalStateException("A configuração da grade deve conter turnos, períodos e dias válidos.");
        }
        return configuracao;
    }

    private void configurarDisponibilidade(
            Professor professor,
            List<String> horariosIndisponiveis,
            TamanhoGrade configuracao) {
        Boolean[][][] disponibilidade = ajustarDisponibilidade(
                null,
                configuracao.getQtdeTurnos(),
                configuracao.getQtdePeriodosDia(),
                configuracao.getQtdeDiasSemana());

        if (horariosIndisponiveis != null) {
            for (String horario : horariosIndisponiveis) {
                String[] coordenadas = horario.split(":", -1);
                if (coordenadas.length != 3) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário indisponível inválido.");
                }

                try {
                    int turno = Integer.parseInt(coordenadas[0]);
                    int periodo = Integer.parseInt(coordenadas[1]);
                    int dia = Integer.parseInt(coordenadas[2]);
                    if (turno < 0 || turno >= disponibilidade.length
                            || periodo < 0 || periodo >= disponibilidade[turno].length
                            || dia < 0 || dia >= disponibilidade[turno][periodo].length) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário indisponível fora da grade.");
                    }
                    disponibilidade[turno][periodo][dia] = false;
                } catch (NumberFormatException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horário indisponível inválido.", exception);
                }
            }
        }

        professor.setDisponibilidadeHoraria(disponibilidade);
    }

    private Boolean[][][] ajustarDisponibilidade(
            Boolean[][][] existente,
            int quantidadeTurnos,
            int quantidadePeriodos,
            int quantidadeDias) {
        Boolean[][][] ajustada = new Boolean[quantidadeTurnos][quantidadePeriodos][quantidadeDias];
        for (int turno = 0; turno < quantidadeTurnos; turno++) {
            for (int periodo = 0; periodo < quantidadePeriodos; periodo++) {
                for (int dia = 0; dia < quantidadeDias; dia++) {
                    ajustada[turno][periodo][dia] = true;
                    if (existente != null
                            && turno < existente.length
                            && existente[turno] != null
                            && periodo < existente[turno].length
                            && existente[turno][periodo] != null
                            && dia < existente[turno][periodo].length
                            && existente[turno][periodo][dia] != null) {
                        ajustada[turno][periodo][dia] = existente[turno][periodo][dia];
                    }
                }
            }
        }
        return ajustada;
    }
}
