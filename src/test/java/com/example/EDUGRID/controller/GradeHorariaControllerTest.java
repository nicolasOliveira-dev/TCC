package com.example.EduGrid.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.service.GradeHorariaService;
import com.example.EduGrid.service.TamanhoGradeService;
import com.example.EduGrid.service.TurmaService;
import com.example.EduGrid.solver.service.AcompanhamentoOtimizacaoService;

class GradeHorariaControllerTest {

    @Test
    void classificaTurmasPeloTurnoDoCursoIgnorandoAcentosECaixa() {
        TamanhoGrade grade = new TamanhoGrade(3, 5, 5);
        List<Turma> turmas = List.of(
                turmaNoTurno("Matutino", grade),
                turmaNoTurno("MANHÃ", grade),
                turmaNoTurno("Vespertino", grade),
                turmaNoTurno(" tarde ", grade),
                turmaNoTurno("Noite", grade),
                turmaNoTurno("NOTURNO", grade),
                turmaNoTurno("Integral", grade));

        TamanhoGradeService tamanhoGradeService = mock(TamanhoGradeService.class);
        when(tamanhoGradeService.buscarConfiguracao()).thenReturn(grade);
        TurmaService turmaService = mock(TurmaService.class);
        when(turmaService.listarTurmasParaOtimizacao()).thenReturn(turmas);
        GradeHoraria gradeGerada = new GradeHoraria(turmas, grade);
        gradeGerada.setId(1);
        GradeHorariaService gradeHorariaService = mock(GradeHorariaService.class);
        when(gradeHorariaService.listarGradesGeradas()).thenReturn(List.of(gradeGerada));
        GradeHorariaController controller = new GradeHorariaController(
                gradeHorariaService,
                tamanhoGradeService,
                turmaService,
                mock(AcompanhamentoOtimizacaoService.class));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.visualizarGrade(model);

        assertEquals("Grade/grade", view);
        assertEquals(List.of(0, 0, 1, 1, 2, 2, -1), model.get("turnosTurmas"));
    }

    @Test
    void abrirGeradorExibeHistoricoEListaVazia() {
        GradeHorariaService gradeHorariaService = mock(GradeHorariaService.class);
        when(gradeHorariaService.listarGradesGeradas()).thenReturn(List.of());
        GradeHorariaController controller = new GradeHorariaController(
                gradeHorariaService,
                mock(TamanhoGradeService.class),
                mock(TurmaService.class),
                mock(AcompanhamentoOtimizacaoService.class));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.abrirGerador(model);

        assertEquals("Grade/listar", view);
        assertEquals(List.of(), model.get("gradesGeradas"));
    }

    @Test
    void envioDaConfiguracaoIniciaGeracaoComTurmasAtuais() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(2, 5, 5);
        List<Turma> turmas = List.of(turmaNoTurno("Matutino", tamanhoGrade));
        TamanhoGradeService tamanhoGradeService = mock(TamanhoGradeService.class);
        when(tamanhoGradeService.salvar(tamanhoGrade)).thenReturn(tamanhoGrade);
        TurmaService turmaService = mock(TurmaService.class);
        when(turmaService.listarTurmas()).thenReturn(turmas);
        AcompanhamentoOtimizacaoService acompanhamentoService =
                mock(AcompanhamentoOtimizacaoService.class);
        UUID jobId = UUID.randomUUID();
        when(acompanhamentoService.iniciar(
                eq(turmas),
                eq(tamanhoGrade),
                any(GradeHoraria.class)
        )).thenReturn(jobId);
        GradeHorariaController controller = new GradeHorariaController(
                mock(GradeHorariaService.class),
                tamanhoGradeService,
                turmaService,
                acompanhamentoService
        );
        GradeHoraria formulario = new GradeHoraria();
        formulario.setTamanhoGrade(tamanhoGrade);

        String destino = controller.gerarGrade(formulario, new ExtendedModelMap());

        assertEquals("redirect:/gradeHoraria/otimizando/" + jobId, destino);
    }

    private Turma turmaNoTurno(String turno, TamanhoGrade grade) {
        Curso curso = new Curso("Curso", "Tipo", turno);
        Turma turma = new Turma();
        turma.setNome("Turma " + turno);
        turma.setCurso(curso);
        turma.setGradeHorariaMatriz(grade);
        return turma;
    }
}
