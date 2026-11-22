package com.example.EduGrid.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException, ServletException {
        String caminho = request.getServletPath();
        if (caminho.equals("/") || caminho.equals("/cadastro") || caminho.equals("/login")
                || caminho.equals("/sair") || caminho.equals("/error")
                || caminho.startsWith("/css/") || caminho.startsWith("/js/")
                || caminho.startsWith("/planilhas/") || caminho.startsWith("/images/")) {
            return true;
        }
        if (request.getSession(false) != null
                && request.getSession(false).getAttribute("usuarioId") != null) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/");
        return false;
    }
}
