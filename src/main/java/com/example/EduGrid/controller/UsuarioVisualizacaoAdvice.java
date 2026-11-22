package com.example.EduGrid.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.EduGrid.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice(annotations = Controller.class)
public class UsuarioVisualizacaoAdvice {

    @ModelAttribute("nomeUsuario")
    public String nomeUsuario(HttpSession session) {
        Object nome = session.getAttribute("nomeUsuario");
        return nome == null ? "EduGrid" : nome.toString();
    }

    @ModelAttribute("iniciaisUsuario")
    public String iniciaisUsuario(HttpSession session) {
        Object nome = session.getAttribute("nomeUsuario");
        return nome == null ? "EG" : UsuarioService.iniciais(nome.toString());
    }
}
