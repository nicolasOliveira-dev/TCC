package com.example.EduGrid.controller;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.service.GradeEdicaoService;
import com.example.EduGrid.service.GradeHorariaService;
import com.example.EduGrid.service.TamanhoGradeService;
import com.example.EduGrid.service.TurmaService;
import com.example.EduGrid.solver.service.AcompanhamentoOtimizacaoService;
import com.example.EduGrid.solver.service.NovaVersaoGradeService;

@Controller
@RequestMapping("/gradeHoraria")
public class GradeHorariaController {

    private final GradeHorariaService gradeHorariaService;
    private final TamanhoGradeService tamanhoGradeService;
    private final TurmaService turmaService;
    private final AcompanhamentoOtimizacaoService acompanhamentoOtimizacaoService;
    private final GradeEdicaoService gradeEdicaoService;
    private final NovaVersaoGradeService novaVersaoGradeService;

    @Autowired
    public GradeHorariaController(
            GradeHorariaService gradeHorariaService,
            TamanhoGradeService tamanhoGradeService,
            TurmaService turmaService,
            AcompanhamentoOtimizacaoService acompanhamentoOtimizacaoService,
            GradeEdicaoService gradeEdicaoService,
            NovaVersaoGradeService novaVersaoGradeService) {

        this.gradeHorariaService = gradeHorariaService;
        this.tamanhoGradeService = tamanhoGradeService;
        this.turmaService = turmaService;
        this.acompanhamentoOtimizacaoService
                = acompanhamentoOtimizacaoService;
        this.gradeEdicaoService = gradeEdicaoService;
        this.novaVersaoGradeService = novaVersaoGradeService;
    }

    public GradeHorariaController(
            GradeHorariaService gradeHorariaService,
            TamanhoGradeService tamanhoGradeService,
            TurmaService turmaService,
            AcompanhamentoOtimizacaoService acompanhamentoOtimizacaoService) {
        this(gradeHorariaService, tamanhoGradeService, turmaService,
                acompanhamentoOtimizacaoService, null, null);
    }

    @GetMapping("/gerar")
    public String abrirGerador(Model model) {
        model.addAttribute("gradesGeradas", gradeHorariaService.listarGradesGeradas());
        return "Grade/listar";
    }

    @GetMapping("/configurar")
    public String abrirFormularioConfiguracao(Model model) {
        model.addAttribute("tamanhoGrade", tamanhoGradeService.buscarConfiguracao());
        return "Grade/gerar";
    }

    @PostMapping("/gerar")
    public String gerarGrade(@ModelAttribute GradeHoraria gradeHoraria, Model model) {
        TamanhoGrade tamanhoInformado = gradeHoraria.getTamanhoGrade();
        TamanhoGrade configuracaoAtual = tamanhoGradeService.salvar(tamanhoInformado);
        List<Turma> turmas = turmaService.listarTurmasParaOtimizacao();
        GradeHoraria gradeGerada = new GradeHoraria(turmas, configuracaoAtual);

        UUID id = acompanhamentoOtimizacaoService.iniciar(
                turmas,
                configuracaoAtual,
                gradeGerada
        );

        return "redirect:/gradeHoraria/otimizando/" + id;
    }

    @GetMapping("/grade")
    public String visualizarGrade(Model model) {
        List<GradeHoraria> grades = gradeHorariaService.listarGradesGeradas();
        if (grades == null || grades.isEmpty()) {
            return "redirect:/gradeHoraria/gerar";
        }

        return exibirGrade(grades.get(0), model);
    }

    @GetMapping("/grade/{id}")
    public String visualizarGradeGerada(@PathVariable Integer id, Model model) {
        GradeHoraria gradeHoraria = gradeHorariaService.buscarGradeGerada(id);
        if (gradeHoraria == null) {
            return "redirect:/gradeHoraria/gerar";
        }

        return exibirGrade(gradeHoraria, model);
    }

    @GetMapping("/otimizando/{id}")
    public String acompanharOtimizacao(
            @PathVariable UUID id,
            Model model) {

        model.addAttribute("jobId", id);

        return "Grade/otimizando";
    }

    @PostMapping("/grade/{id}/nova-versao")
    public String iniciarNovaVersao(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean preservarEdicoesManuais) {
        UUID tentativa = novaVersaoGradeService.iniciar(id, preservarEdicoesManuais);
        return "redirect:/gradeHoraria/nova-versao/" + tentativa;
    }

    @GetMapping("/nova-versao/{id}")
    public String acompanharNovaVersao(@PathVariable UUID id, Model model) {
        model.addAttribute("tentativaId", id);
        return "Grade/nova-versao";
    }

    @GetMapping("/nova-versao/{id}/status")
    @ResponseBody
    public Map<String, Object> consultarNovaVersao(@PathVariable UUID id) {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("status", novaVersaoGradeService.status(id));
        resposta.put("resultado", novaVersaoGradeService.resumo(id));
        return resposta;
    }

    @PostMapping("/nova-versao/{id}/aplicar")
    public String aplicarNovaVersao(@PathVariable UUID id, Model model) {
        try {
            Integer gradeId = novaVersaoGradeService.aplicar(id);
            return "redirect:/gradeHoraria/grade/" + gradeId;
        } catch (IllegalStateException e) {
            model.addAttribute("tentativaId", id);
            model.addAttribute("erroAplicacao", e.getMessage());
            return "Grade/nova-versao";
        }
    }

    @PostMapping("/nova-versao/{id}/manter")
    public String manterGradeAtual(@PathVariable UUID id) {
        var resumo = novaVersaoGradeService.resumo(id);
        if (resumo == null) {
            return "redirect:/gradeHoraria/gerar";
        }
        novaVersaoGradeService.descartar(id);
        return "redirect:/gradeHoraria/grade/" + resumo.gradeOrigemId();
    }

    @GetMapping("/otimizacao/{id}/status")
    @ResponseBody
    public Map<String, Object> consultarStatus(
            @PathVariable UUID id) {

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("status", acompanhamentoOtimizacaoService.consultarStatus(id));
        resultado.put("gradeId", acompanhamentoOtimizacaoService.consultarGradeId(id));
        resultado.put("erro", acompanhamentoOtimizacaoService.consultarErro(id));
        return resultado;
    }

    @PostMapping("/grade/{id}/prever-edicao")
    @ResponseBody
    public ResponseEntity<GradeEdicaoService.Resultado> preverEdicao(
            @PathVariable Integer id,
            @RequestBody GradeEdicaoService.Movimento movimento) {
        return executarEdicao(id, movimento, false);
    }

    @PostMapping("/grade/{id}/edicoes")
    @ResponseBody
    public ResponseEntity<GradeEdicaoService.Resultado> salvarEdicao(
            @PathVariable Integer id,
            @RequestBody GradeEdicaoService.Movimento movimento) {
        return executarEdicao(id, movimento, true);
    }

    private ResponseEntity<GradeEdicaoService.Resultado> executarEdicao(
            Integer id, GradeEdicaoService.Movimento movimento, boolean salvar) {
        try {
            GradeEdicaoService.Resultado resultado = gradeEdicaoService.mover(id, movimento, salvar);
            return ResponseEntity.status(resultado.valido() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY).body(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GradeEdicaoService.Resultado(
                    false, false, e.getMessage(), List.of(e.getMessage()), List.of(), 0));
        }
    }

    private String exibirGrade(
            GradeHoraria gradeHoraria,
            Model model) {

        gradeHorariaService.carregarHorariosPersistidos(
                gradeHoraria
        );

        gradeHoraria.gerarMatriz();

        List<Integer> turnosTurmas
                = gradeHoraria.getTurmas() == null
                ? List.of()
                : gradeHoraria.getTurmas().stream()
                        .map(turma -> classificarTurno(
                                turma == null
                                || turma.getCurso() == null
                                ? null
                                : turma.getCurso().getTurno()
                        ))
                        .toList();

        model.addAttribute("grade", gradeHoraria);
        model.addAttribute("relatorioRestricoesHard", gradeHoraria.getRelatorioRestricoesHard());
        model.addAttribute("relatorioEdicao", gradeHoraria.getRelatorioEdicao());
        model.addAttribute("turnosTurmas", turnosTurmas);
        model.addAttribute("horariosGrade", gradeEdicaoService == null
                ? List.of() : gradeEdicaoService.listarParaTela(gradeHoraria));

        return "Grade/grade";
    }

    private int classificarTurno(String turno) {
        if (turno == null) {
            return -1;
        }

        String normalizado = Normalizer.normalize(turno, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (normalizado.contains("matutino") || normalizado.contains("manha")) {
            return 0;
        }
        if (normalizado.contains("vespertino") || normalizado.contains("tarde")) {
            return 1;
        }
        if (normalizado.contains("noturno") || normalizado.contains("noite")) {
            return 2;
        }
        return -1;
    }
}
