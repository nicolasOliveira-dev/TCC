package com.example.EduGrid.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.service.DisciplinaService;
import com.example.EduGrid.service.ProfessorService;
import com.example.EduGrid.service.TamanhoGradeService;

class ProfessorControllerTest {

    private ProfessorService professorService;
    private TamanhoGradeService tamanhoGradeService;
    private DisciplinaService disciplinaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        professorService = org.mockito.Mockito.mock(ProfessorService.class);
        tamanhoGradeService = org.mockito.Mockito.mock(TamanhoGradeService.class);
        disciplinaService = org.mockito.Mockito.mock(DisciplinaService.class);
        when(tamanhoGradeService.buscarConfiguracao()).thenReturn(new TamanhoGrade(2, 3, 5));
        when(disciplinaService.listarDisciplinas()).thenReturn(List.of());
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProfessorController(professorService, tamanhoGradeService, disciplinaService)).build();
    }

    @Test
    void salvarConverteHorariosMarcadosEmIndisponiveis() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/professor/salvar")
                .param("nome", "Ana")
                .param("areaAtuacao", "Matemática")
                .param("cargaHoraria", "30")
                .param("intencaoDiaPlanejamento", "Segunda-feira", "Quarta-feira")
                .param("horariosIndisponiveis", "1:2:3", "0:0:1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/professor/professor"));

        ArgumentCaptor<Professor> captor = ArgumentCaptor.forClass(Professor.class);
        verify(professorService).salvarProfessor(captor.capture());
        Professor salvo = captor.getValue();

        assertEquals(List.of("Segunda-feira", "Quarta-feira"), salvo.getIntencaoDiaPlanejamento());
        assertEquals(2, salvo.getDisponibilidadeHoraria().length);
        assertEquals(3, salvo.getDisponibilidadeHoraria()[0].length);
        assertEquals(5, salvo.getDisponibilidadeHoraria()[0][0].length);
        assertFalse(salvo.getDisponibilidadeHoraria()[1][2][3]);
        assertFalse(salvo.getDisponibilidadeHoraria()[0][0][1]);
        assertTrue(salvo.getDisponibilidadeHoraria()[1][0][0]);
    }

    @Test
    void atualizarSubstituiDisponibilidadePelosHorariosMarcados() throws Exception {
        Disciplina matematica = disciplina(3, "Geometria", "Matemática");
        when(disciplinaService.listarDisciplinas()).thenReturn(List.of(matematica));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/professor/atualizar")
                .param("id", "7")
                .param("nome", "Ana")
                .param("areaAtuacao", "Matemática")
                .param("cargaHoraria", "30")
                .param("horariosIndisponiveis", "0:1:4")
                .param("disciplinasSelecionadas", "3"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/professor/professor"));

        ArgumentCaptor<Professor> captor = ArgumentCaptor.forClass(Professor.class);
        verify(professorService).atualizarProfessor(eq(7), captor.capture());
        assertFalse(captor.getValue().getDisponibilidadeHoraria()[0][1][4]);
        assertTrue(captor.getValue().getDisponibilidadeHoraria()[0][1][3]);
        assertEquals(List.of(matematica), captor.getValue().getDisciplinasPossiveis());
    }

    @Test
    void salvarAssociaSomenteDisciplinasDaAreaDeAtuacao() throws Exception {
        Disciplina matematica = disciplina(1, "Álgebra", "Matemática");
        when(disciplinaService.listarDisciplinas()).thenReturn(List.of(
                matematica,
                disciplina(2, "Física", "Ciências")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/professor/salvar")
                .param("nome", "Ana")
                .param("areaAtuacao", "matematica")
                .param("cargaHoraria", "30")
                .param("disciplinasSelecionadas", "1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/professor/professor"));

        ArgumentCaptor<Professor> captor = ArgumentCaptor.forClass(Professor.class);
        verify(professorService).salvarProfessor(captor.capture());
        assertEquals(List.of(matematica), captor.getValue().getDisciplinasPossiveis());
    }

    @Test
    void rejeitaDisciplinaSelecionadaDeOutraArea() throws Exception {
        when(disciplinaService.listarDisciplinas()).thenReturn(List.of(
                disciplina(2, "Física", "Ciências")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/professor/salvar")
                .param("nome", "Ana")
                .param("areaAtuacao", "Matemática")
                .param("cargaHoraria", "30")
                .param("disciplinasSelecionadas", "2"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void abrirFormularioInicializaTodasAsCelulasComoDisponiveis() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/professor/salvar"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeExists(
                        "diasSemana", "nomesTurnos", "quantidadePeriodos", "professor",
                        "disciplinas", "disciplinasSelecionadas"));
    }

    private Disciplina disciplina(Integer id, String nome, String areaAtuacao) {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(id);
        disciplina.setNome(nome);
        disciplina.setAreaAtuacao(areaAtuacao);
        return disciplina;
    }
}
