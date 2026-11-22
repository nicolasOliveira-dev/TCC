package com.example.EduGrid.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.EduGrid.service.ImportacaoService;

@Controller
@RequestMapping("/importacao")
public class ImportacaoController {

    private final ImportacaoService importacaoService;

    public ImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    @GetMapping
    public String abrirFormulario() {
        return "importacao/importar";
    }

    @PostMapping
    public String importar(
            @RequestParam("arquivo") MultipartFile arquivo,
            Model model) {

        try {
            var resultado = importacaoService.importar(arquivo);
            model.addAttribute("resultado", resultado);

        } catch (IllegalArgumentException | IOException e) {
            model.addAttribute("erro", e.getMessage());

        } catch (Exception e) {
            model.addAttribute("erro",
                    "Não foi possível concluir a importação.");
        }

        return "importacao/importar";
    }
}
