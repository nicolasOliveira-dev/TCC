package com.example.EduGrid.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.service.GradeEdicaoService;
import com.example.EduGrid.service.GradeHorariaService;
import com.example.EduGrid.service.TamanhoGradeService;
import com.example.EduGrid.service.TurmaService;
import com.example.EduGrid.solver.service.AcompanhamentoOtimizacaoService;
import com.example.EduGrid.solver.service.NovaVersaoGradeService;

@WebMvcTest(GradeHorariaController.class)
@TestPropertySource(properties = "edugrid.dados.adequar-na-inicializacao=false")
class GradeFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeHorariaService gradeHorariaService;
    @MockitoBean
    private TamanhoGradeService tamanhoGradeService;
    @MockitoBean
    private TurmaService turmaService;
    @MockitoBean
    private AcompanhamentoOtimizacaoService acompanhamentoOtimizacaoService;
    @MockitoBean
    private GradeEdicaoService gradeEdicaoService;
    @MockitoBean
    private NovaVersaoGradeService novaVersaoGradeService;

    @Test
    void gradeExibeAcoesDeEdicaoENovaVersao() throws Exception {
        TamanhoGrade tamanho = new TamanhoGrade(3, 5, 5);
        Turma turma = new Turma();
        turma.setId(1);
        turma.setNome("Turma A");
        turma.setCurso(new Curso("Curso A", "Técnico", "Matutino"));
        turma.setGradeHorariaMatriz(tamanho);
        GradeHoraria grade = new GradeHoraria(List.of(turma), tamanho);
        grade.setId(1);
        when(gradeHorariaService.buscarGradeGerada(1)).thenReturn(grade);
        when(gradeEdicaoService.listarParaTela(grade)).thenReturn(List.of());

        mockMvc.perform(get("/gradeHoraria/grade/1").sessionAttr("usuarioId", 1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar grade")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gerar nova vers")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/gradeHoraria/grade/1/nova-versao")));
    }

    @Test
    void previsaoEConfirmacaoDaEdicaoPassamPeloServicoERespondemComEstado() throws Exception {
        var movimento = new GradeEdicaoService.Movimento(1, 0, 0, 1, 0, false);
        when(gradeEdicaoService.mover(1, movimento, false)).thenReturn(
                new GradeEdicaoService.Resultado(true, false, "Prévia válida.", List.of(), List.of(), 1));
        when(gradeEdicaoService.mover(1, movimento, true)).thenReturn(
                new GradeEdicaoService.Resultado(true, true, "Alteração salva.", List.of(), List.of(), 1));
        String json = """
                {"turmaId":1,"diaOrigem":0,"periodoOrigem":0,"diaDestino":1,"periodoDestino":0,"confirmarSoft":false}
                """;

        mockMvc.perform(post("/gradeHoraria/grade/1/prever-edicao")
                        .sessionAttr("usuarioId", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.salvo").value(false));

        mockMvc.perform(post("/gradeHoraria/grade/1/edicoes")
                        .sessionAttr("usuarioId", 1)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.salvo").value(true));

        verify(gradeEdicaoService).mover(1, movimento, false);
        verify(gradeEdicaoService).mover(1, movimento, true);
    }

    @Test
    void aplicarNovaVersaoRedirecionaParaAVisualizacaoDaGradePersistida() throws Exception {
        UUID tentativa = UUID.randomUUID();
        GradeHoraria novaGrade = grade(9);
        when(novaVersaoGradeService.aplicar(tentativa)).thenReturn(9);
        when(gradeHorariaService.buscarGradeGerada(9)).thenReturn(novaGrade);
        when(gradeEdicaoService.listarParaTela(novaGrade)).thenReturn(List.of());

        mockMvc.perform(post("/gradeHoraria/nova-versao/{id}/aplicar", tentativa)
                        .sessionAttr("usuarioId", 1))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/gradeHoraria/grade/9"));

        verify(novaVersaoGradeService).aplicar(eq(tentativa));

        mockMvc.perform(get("/gradeHoraria/grade/9").sessionAttr("usuarioId", 1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("#9")));
    }

    private GradeHoraria grade(Integer id) {
        TamanhoGrade tamanho = new TamanhoGrade(3, 5, 5);
        Turma turma = new Turma();
        turma.setId(1);
        turma.setNome("Turma A");
        turma.setCurso(new Curso("Curso A", "Técnico", "Matutino"));
        turma.setGradeHorariaMatriz(tamanho);
        GradeHoraria grade = new GradeHoraria(List.of(turma), tamanho);
        grade.setId(id);
        return grade;
    }
}
