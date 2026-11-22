package com.example.EduGrid.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.service.DisciplinaService;
import com.example.EduGrid.service.ProfessorService;
import com.example.EduGrid.service.TamanhoGradeService;

@WebMvcTest(ProfessorController.class)
@TestPropertySource(properties = "edugrid.dados.adequar-na-inicializacao=false")
class ProfessorFormRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfessorService professorService;

    @MockitoBean
    private DisciplinaService disciplinaService;

    @MockitoBean
    private TamanhoGradeService tamanhoGradeService;

    @BeforeEach
    void configurarGrade() {
        when(tamanhoGradeService.buscarConfiguracao()).thenReturn(new TamanhoGrade(2, 3, 5));
        when(disciplinaService.listarDisciplinas()).thenReturn(java.util.List.of(
                disciplina(1, "Álgebra", "Matemática"),
                disciplina(2, "Física", "Ciências")));
    }

    @Test
    void renderizaFormularioDeCadastroComMatrizPorTurno() throws Exception {
        mockMvc.perform(get("/professor/salvar").sessionAttr("usuarioId", 1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dia de planejamento desejado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Manhã")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tarde")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quarta-feira")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("disciplinasSelecionadas")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Álgebra")));
    }

    @Test
    void renderizaFormularioDeAtualizacaoComDisponibilidadeSalva() throws Exception {
        Professor professor = new Professor();
        professor.setId(8);
        professor.setNome("Ana");
        professor.setAreaAtuacao("Matemática");
        professor.setCargaHoraria(30);
        professor.setDisciplinasPossiveis(java.util.List.of(disciplina(1, "Álgebra", "Matemática")));
        professor.setDisponibilidadeHoraria(new Boolean[][][] {
                {{true, true, true, true, true}, {true, true, true, true, true}, {true, true, true, true, true}},
                {{true, true, true, true, true}, {true, true, true, true, true}, {true, true, false, true, true}}
        });
        when(professorService.buscarProfessorPorId(8)).thenReturn(professor);

        mockMvc.perform(get("/professor/atualizar").param("id", "8").sessionAttr("usuarioId", 1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"1:2:2\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("checked=\"checked\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"1\"")));
    }

    private Disciplina disciplina(Integer id, String nome, String areaAtuacao) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(id);
        disciplina.setNome(nome);
        disciplina.setAreaAtuacao(areaAtuacao);
        return disciplina;
    }
}
