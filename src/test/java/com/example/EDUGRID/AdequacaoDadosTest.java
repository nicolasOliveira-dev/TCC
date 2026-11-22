package com.example.EduGrid;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.example.EduGrid.model.*;
import com.example.EduGrid.service.*;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.service.OtimizacaoService;

class AdequacaoDadosTest {
    @Test void completaMatrizSemInventarDisponibilidadeOuApagarPosicoes() {
        Boolean[][][] original = {{{true, false, null}, {false, true, false}, {true, false, true}}};
        var ajustada = EduGridApplication.completarDisponibilidade(original, 1, 2, 4);
        assertEquals(3, ajustada[0].length);
        assertEquals(4, ajustada[0][0].length);
        assertTrue(ajustada[0][0][0]);
        assertFalse(ajustada[0][0][1]);
        assertFalse(ajustada[0][0][2]);
        assertFalse(ajustada[0][0][3]);
        assertTrue(ajustada[0][2][2]);
        assertNull(original[0][0][2]);
    }

    @Test void atualizaPelosServicesESegundaExecucaoNaoRepeteGravacoes() {
        var professores = mock(ProfessorService.class);
        var disciplinas = mock(DisciplinaService.class);
        var turmas = mock(TurmaService.class);
        var configuracoes = mock(TamanhoGradeService.class);
        var otimizacao = mock(OtimizacaoService.class);
        var tamanho = new TamanhoGrade(1, 5, 5);
        tamanho.setId(1);
        var disciplina = new Disciplina(1, "Matematica", "Geral", 1.5, null, null, "comum");
        var professor = new Professor();
        professor.setDisponibilidadeHoraria(new Boolean[][][]{{{true, false}}});
        var turma = new Turma();
        turma.setId(1);
        when(configuracoes.buscarConfiguracao()).thenReturn(tamanho);
        when(disciplinas.listarDisciplinas()).thenReturn(List.of(disciplina));
        when(professores.listarProfessores()).thenReturn(List.of(professor));
        when(turmas.listarTurmas()).thenReturn(List.of(turma));
        when(otimizacao.montarProblema(List.of(turma), tamanho))
                .thenReturn(new GradePlanejamento(List.of(), List.of(), List.of()));

        for (int i = 0; i < 2; i++) {
            EduGridApplication.adequarDados(professores, disciplinas, turmas, configuracoes,
                    new BlocoDisciplinaService(), otimizacao);
        }
        verify(disciplinas, times(1)).salvarDisciplina(disciplina);
        verify(professores, times(1)).salvarProfessor(professor);
        verify(turmas, times(1)).salvarTurma(turma);
        assertEquals(2, disciplina.getNumPeriodos());
        assertEquals(1, disciplina.getMinPeriodosJuntos());
        assertEquals(2, disciplina.getMaxPeriodosJuntos());
        assertEquals(1.5, disciplina.getCargaHoraria());
        assertTrue(professor.getDisponibilidadeHoraria()[0][0][0]);
        assertFalse(professor.getDisponibilidadeHoraria()[0][4][4]);
    }

    @Test void naoExecutaAtualizacoesSemConfiguracaoValida() {
        var professores = mock(ProfessorService.class);
        var disciplinas = mock(DisciplinaService.class);
        var turmas = mock(TurmaService.class);
        var configuracoes = mock(TamanhoGradeService.class);
        assertThrows(IllegalStateException.class, () -> EduGridApplication.adequarDados(
                professores, disciplinas, turmas, configuracoes, new BlocoDisciplinaService(),
                mock(OtimizacaoService.class)));
        verifyNoInteractions(professores, disciplinas, turmas);
    }
}
