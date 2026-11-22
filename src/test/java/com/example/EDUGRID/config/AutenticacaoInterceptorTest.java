package com.example.EduGrid.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AutenticacaoInterceptorTest {

    private final AutenticacaoInterceptor interceptor = new AutenticacaoInterceptor();

    @Test
    void redirecionaAcessoAoPainelSemSessao() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals("/", response.getRedirectedUrl());
    }

    @Test
    void liberaRotasPublicasESessaoAutenticada() throws Exception {
        MockHttpServletRequest cadastro = new MockHttpServletRequest("GET", "/cadastro");
        cadastro.setServletPath("/cadastro");
        MockHttpServletResponse respostaCadastro = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(cadastro, respostaCadastro, new Object()));

        MockHttpServletRequest painel = new MockHttpServletRequest("GET", "/dashboard");
        painel.setServletPath("/dashboard");
        painel.getSession().setAttribute("usuarioId", 1);
        MockHttpServletResponse respostaPainel = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(painel, respostaPainel, new Object()));
    }
}
