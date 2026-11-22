package com.example.EduGrid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.EduGrid.model.Professor;

public interface ProfessorRepository
        extends JpaRepository<Professor, Integer> {

    @EntityGraph(attributePaths = "disciplinasPossiveis")
    @Query("SELECT DISTINCT p FROM Professor p")
    List<Professor> findAllComDisciplinasPossiveis();

    @Query("""
            SELECT AVG(p.cargaHoraria)
            FROM Professor p
            """)
    Double calcularMediaCargaHoraria();

    @Query("""
            SELECT p
            FROM Professor p
            WHERE p.cargaHoraria >
                  (SELECT AVG(pr.cargaHoraria)
                   FROM Professor pr)
            """)
    List<Professor> professoresAcimaDaMedia();

    @Query("""
            SELECT p.areaAtuacao, COUNT(p)
            FROM Professor p
            GROUP BY p.areaAtuacao
            """)
    List<Object[]> contarProfessoresPorAreaAtuacao();

    @Query("""
            SELECT p
            FROM Professor p
            WHERE p.cargaHoraria = (
                SELECT MAX(pr.cargaHoraria)
                FROM Professor pr
            )
            """)
    List<Professor> professorComMaiorCargaHoraria();
}
