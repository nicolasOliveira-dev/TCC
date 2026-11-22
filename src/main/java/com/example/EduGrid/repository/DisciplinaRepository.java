package com.example.EduGrid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.example.EduGrid.model.Disciplina;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Integer> {
}
