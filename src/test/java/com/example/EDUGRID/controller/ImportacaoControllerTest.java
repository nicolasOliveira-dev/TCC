
package com.example.EduGrid.controller;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.example.EduGrid.service.ImportacaoService;
import com.example.EduGrid.service.ImportacaoService.ResultadoImportacao;

class ImportacaoControllerTest {

    private ImportacaoService service;
    private ImportacaoController controller;
    private Model model;

    @BeforeEach
    void configurar() {
        service = mock(ImportacaoService.class);
        controller = new ImportacaoController(service);
        model = new ExtendedModelMap();
    }

    private MockMultipartFile arquivo() {
        return new MockMultipartFile(
                "arquivo",
                "dados.xlsx",
                "application/vnd.openxmlformats-officedocument."
                        + "spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );
    }

    @Test
    void deveAbrirFormulario() {
        assertEquals(
                "importacao/importar",
                controller.abrirFormulario()
        );
    }

    @Test
    void deveExibirResultadoQuandoImportacaoForBemSucedida()
            throws Exception {

        MockMultipartFile arquivo = arquivo();

        ResultadoImportacao resultado =
                new ResultadoImportacao(2, 5, 3, 4);

        when(service.importar(arquivo)).thenReturn(resultado);

        String pagina = controller.importar(arquivo, model);

        assertEquals("importacao/importar", pagina);
        assertSame(resultado, model.getAttribute("resultado"));
        assertNull(model.getAttribute("erro"));

        verify(service).importar(arquivo);
    }

    @Test
    void deveExibirErroDeValidacao() throws Exception {
        MockMultipartFile arquivo = arquivo();

        when(service.importar(arquivo))
                .thenThrow(new IllegalArgumentException(
                        "Aba Cursos, linha 2: Código duplicado"));

        String pagina = controller.importar(arquivo, model);

        assertEquals("importacao/importar", pagina);
        assertEquals(
                "Aba Cursos, linha 2: Código duplicado",
                model.getAttribute("erro")
        );
        assertNull(model.getAttribute("resultado"));
    }

    @Test
    void deveExibirErroDeLeitura() throws Exception {
        MockMultipartFile arquivo = arquivo();

        when(service.importar(arquivo))
                .thenThrow(new IOException(
                        "Falha ao ler a planilha"));

        controller.importar(arquivo, model);

        assertEquals(
                "Falha ao ler a planilha",
                model.getAttribute("erro")
        );
    }

    @Test
    void deveExibirMensagemGenericaParaErroInesperado()
            throws Exception {

        MockMultipartFile arquivo = arquivo();

        when(service.importar(arquivo))
                .thenThrow(new RuntimeException(
                        "Erro interno do banco"));

        controller.importar(arquivo, model);

        assertEquals(
                "Não foi possível concluir a importação.",
                model.getAttribute("erro")
        );
    }
}
