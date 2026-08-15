package com.example.EduGrid.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import com.example.EduGrid.model.Disciplina;



public interface DisciplinaRepository
                extends JpaRepository<Disciplina, Integer> {
       @Query("""
                        SELECT d
                        FROM Disciplina d
                        WHERE d.cargaHoraria >
                              (SELECT AVG(di.cargaHoraria)
                               FROM Disciplina di)
                        """)
        List<Disciplina> disciplinasAcimaDaMedia();

        @Query("""
                        SELECT SUM(cargaHoraria)
                        FROM Disciplina d
                        """)
        Integer calcularCargaHorariaTotal();

        @Query("""
                        SELECT d.historicoProfessor, COUNT(d)
                        FROM Disciplina d
                        GROUP BY d.historicoProfessor
                        ORDER BY COUNT(d) DESC
                        """)
        List<Object[]> disciplinasPorProfessor();

        @Query("""
                        SELECT d
                        FROM Disciplina d
                        WHERE d.cargaHoraria = (
                            SELECT MAX(di.cargaHoraria)
                            FROM Disciplina di
                        )
                        """)
        List<Disciplina> disciplinaComMaiorCargaHoraria();
}
