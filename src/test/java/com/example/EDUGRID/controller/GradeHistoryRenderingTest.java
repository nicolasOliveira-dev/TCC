package com.example.EduGrid.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.service.GradeHorariaService;
import com.example.EduGrid.service.GradeEdicaoService;
import com.example.EduGrid.service.TamanhoGradeService;
import com.example.EduGrid.service.TurmaService;
import com.example.EduGrid.solver.service.AcompanhamentoOtimizacaoService;
import com.example.EduGrid.solver.service.NovaVersaoGradeService;

@WebMvcTest(GradeHorariaController.class)
@TestPropertySource(properties = "edugrid.dados.adequar-na-inicializacao=false")
class GradeHistoryRenderingTest {

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
    void renderizaHistoricoComGradeLegadaSemConfiguracaoAssociada() throws Exception {
        GradeHoraria gradeLegada = new GradeHoraria();
        gradeLegada.setId(7);
        when(gradeHorariaService.listarGradesGeradas()).thenReturn(List.of(gradeLegada));

        mockMvc.perform(get("/gradeHoraria/gerar").sessionAttr("usuarioId", 1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Grade 7")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Configuração da grade indisponível.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Visualizar esta grade")));
    }
}
