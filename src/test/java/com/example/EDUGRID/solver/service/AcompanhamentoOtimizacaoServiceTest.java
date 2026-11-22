package com.example.EduGrid.solver.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.TamanhoGrade;

class AcompanhamentoOtimizacaoServiceTest {
    @Test void exposDiagnosticoDaValidacaoSemIniciarBusca() throws InterruptedException {
        var otimizacao = mock(OtimizacaoService.class);
        when(otimizacao.iniciarOtimizacao(any(), any(), any()))
                .thenThrow(new DiagnosticoOtimizacaoException("Turma A: nenhum professor habilitado."));
        var acompanhamento = new AcompanhamentoOtimizacaoService(otimizacao);
        var id = acompanhamento.iniciar(List.of(), new TamanhoGrade(1, 5, 5), new GradeHoraria());
        assertEquals("FALHOU", aguardarConclusao(acompanhamento, id));
        assertEquals("Turma A: nenhum professor habilitado.", acompanhamento.consultarErro(id));
        assertNull(acompanhamento.consultarGradeId(id));
        assertNull(acompanhamento.consultarErro(UUID.randomUUID()));
    }

    @Test void naoExpoeDetalhesInternosDeErrosInesperados() throws InterruptedException {
        var otimizacao = mock(OtimizacaoService.class);
        when(otimizacao.iniciarOtimizacao(any(), any(), any()))
                .thenThrow(new IllegalStateException("Detalhe interno"));
        var acompanhamento = new AcompanhamentoOtimizacaoService(otimizacao);
        var id = acompanhamento.iniciar(List.of(), new TamanhoGrade(1, 5, 5), new GradeHoraria());
        assertEquals("FALHOU", aguardarConclusao(acompanhamento, id));
        assertFalse(acompanhamento.consultarErro(id).contains("Detalhe interno"));
    }

    private String aguardarConclusao(AcompanhamentoOtimizacaoService acompanhamento, UUID id)
            throws InterruptedException {
        for (int tentativa = 0; tentativa < 100; tentativa++) {
            String status = acompanhamento.consultarStatus(id);
            if (!"PROCESSANDO".equals(status)) {
                return status;
            }
            Thread.sleep(10);
        }
        return acompanhamento.consultarStatus(id);
    }
}
