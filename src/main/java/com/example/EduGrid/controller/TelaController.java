package com.example.EduGrid.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class TelaController {
    @GetMapping("/")
    public String abrirLogin() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String abrirDashboard() {
        return "dashboard/dashboard";
    }
    
    
}
