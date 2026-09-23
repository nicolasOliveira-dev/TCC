package com.example.EduGrid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.TurmaGrade;


@SpringBootApplication
public class EduGridApplication {
	public static void main(String[] args) {
		System.out.println("Iniciando a aplicação EduGrid...");
		SpringApplication.run(EduGridApplication.class, args);
	}
}
