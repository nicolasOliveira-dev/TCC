package com.example.EduGrid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.EduGrid.model.Turma;

public interface TurmaRepository
        extends JpaRepository<Turma, Integer> {
   
    @Query("""
            SELECT t.curso, COUNT(t)
            FROM Turma t
            GROUP BY t.curso
            ORDER BY COUNT(t) DESC
            """)
    List<Object[]> turmasPorCurso();

    @Query("""
            SELECT t.historicoProfessor, COUNT(t)
            FROM Turma t
            GROUP BY t.historicoProfessor
            ORDER BY COUNT(t) DESC
            """)
    List<Object[]> professoresComMaisTurmas();

}
