package com.example.EduGrid.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import com.example.EduGrid.model.Sala;

public interface SalaRepository extends JpaRepository<Sala, Integer> {
     @Query("""
            SELECT s.localizacao, COUNT(s)
            FROM Sala s
            GROUP BY s.localizacao
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> contarSalasPorLocalizacao();
}
