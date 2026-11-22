package com.example.EduGrid.controller;

import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.service.DisciplinaService;
import com.example.EduGrid.service.ProfessorService;
import com.example.EduGrid.service.TurmaService;
import com.example.EduGrid.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class TelaController {

    private final ProfessorService professorService;
    private final TurmaService turmaService;
    private final DisciplinaService disciplinaService;
    private final UsuarioService usuarioService;

    @Autowired
    public TelaController(ProfessorService professorService, TurmaService turmaService,
            DisciplinaService disciplinaService, UsuarioService usuarioService) {
        this.professorService = professorService;
        this.turmaService = turmaService;
        this.disciplinaService = disciplinaService;
        this.usuarioService = usuarioService;
    }

    public TelaController(ProfessorService professorService, TurmaService turmaService,
            DisciplinaService disciplinaService) {
        this(professorService, turmaService, disciplinaService, null);
    }

    @GetMapping("/")
    public String abrirLogin() {
        return "index";
    }

    @GetMapping("/cadastro")
    public String abrirCadastro() {
        return "cadastro";
    }

    @PostMapping("/login")
    public String autenticar(
            @RequestParam String email,
            @RequestParam String senha,
            HttpServletRequest request) {
        if (usuarioService == null) {
            return "redirect:/?erro";
        }
        return usuarioService.autenticar(email, senha)
                .map(usuario -> {
                    HttpSession sessaoAnterior = request.getSession(false);
                    if (sessaoAnterior != null) {
                        sessaoAnterior.invalidate();
                    }
                    HttpSession novaSessao = request.getSession(true);
                    novaSessao.setAttribute("usuarioId", usuario.getId());
                    novaSessao.setAttribute("nomeUsuario", usuario.getNomeCompleto());
                    return "redirect:/dashboard";
                })
                .orElse("redirect:/?erro");
    }

    @GetMapping("/sair")
    public String sair(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @PostMapping("/cadastro")
    public String cadastrarUsuario(
            @RequestParam String nomeCompleto,
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam String confirmarSenha,
            RedirectAttributes redirectAttributes) {
        if (usuarioService == null) {
            redirectAttributes.addFlashAttribute("erroCadastro", "O cadastro de usuários não está disponível.");
            return "redirect:/cadastro";
        }
        if (!senha.equals(confirmarSenha)) {
            redirectAttributes.addFlashAttribute("erroCadastro", "As senhas informadas não coincidem.");
            return "redirect:/cadastro";
        }
        try {
            usuarioService.cadastrar(nomeCompleto, email, senha);
            redirectAttributes.addFlashAttribute("cadastroConcluido",
                    "Conta criada. Entre com seu e-mail e senha.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erroCadastro", e.getMessage());
            return "redirect:/cadastro";
        }
    }

    @GetMapping("/dashboard")
    public String abrirDashboard(Model model) {
        var professores = professorService.listarProfessores();
        var turmas = turmaService.listarTurmas();
        var disciplinas = disciplinaService.listarDisciplinas();

        model.addAttribute("professores", professores);
        model.addAttribute("turmas", turmas);
        model.addAttribute("professoresRecentes", professores.stream()
                .sorted(Comparator.comparing(Professor::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).toList());
        model.addAttribute("turmasRecentes", turmas.stream()
                .sorted(Comparator.comparing(Turma::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).toList());
        model.addAttribute("disciplinasRecentes", disciplinas.stream()
                .sorted(Comparator.comparing(Disciplina::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).toList());
        model.addAttribute("totalProfessores", professores.size());
        model.addAttribute("totalTurmas", turmas.size());
        model.addAttribute("totalDisciplinas", disciplinas.size());
        return "dashboard/dashboard";
    }

}
