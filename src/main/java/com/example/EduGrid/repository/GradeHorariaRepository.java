package com.example.EduGrid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.EduGrid.model.GradeHoraria;

public interface GradeHorariaRepository extends JpaRepository<GradeHoraria, Integer> {

    List<GradeHoraria> findAllByOrderByIdDesc();

    java.util.Optional<GradeHoraria> findFirstByOrderByIdDesc();
}

