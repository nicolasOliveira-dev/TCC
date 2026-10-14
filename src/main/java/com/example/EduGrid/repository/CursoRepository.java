package com.example.EduGrid.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.EduGrid.model.Curso;

public interface CursoRepository extends JpaRepository<Curso, Integer> {
}
