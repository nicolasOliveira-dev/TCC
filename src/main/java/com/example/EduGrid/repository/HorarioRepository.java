package com.example.EduGrid.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.EduGrid.model.Horario;

public interface HorarioRepository 
        extends JpaRepository<Horario, Integer> {
    
}
