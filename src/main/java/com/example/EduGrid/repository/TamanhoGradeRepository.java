package com.example.EduGrid.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.EduGrid.model.TamanhoGrade;

public interface TamanhoGradeRepository extends JpaRepository<TamanhoGrade, Integer> {
    Optional<TamanhoGrade> findFirstByOrderByIdDesc();
}
