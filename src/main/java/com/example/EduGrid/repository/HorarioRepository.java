package com.example.EduGrid.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.model.Professor;

public interface HorarioRepository
        extends JpaRepository<Horario, Integer> {

    void deleteByTurma(Turma turma);

    List<Horario> findByTurma(Turma turma);

    @EntityGraph(attributePaths = {"turma", "disciplina", "professor"})
    List<Horario> findByTurmaAndGradeHoraria(Turma turma, GradeHoraria gradeHoraria);

    @EntityGraph(attributePaths = {"turma", "disciplina", "professor"})
    List<Horario> findByTurmaAndGradeHorariaIsNull(Turma turma);

    @EntityGraph(attributePaths = {"turma", "disciplina", "professor"})
    List<Horario> findByProfessorAndGradeHoraria(Professor professor, GradeHoraria gradeHoraria);

    @EntityGraph(attributePaths = {"turma", "disciplina", "professor"})
    List<Horario> findByProfessorAndGradeHorariaIsNull(Professor professor);

    List<Horario> findByTurmaIn(Collection<Turma> turmas);

    List<Horario> findByTurmaInAndGradeHorariaIsNull(Collection<Turma> turmas);

    List<Horario> findByGradeHoraria(GradeHoraria gradeHoraria);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Horario h where h.gradeHoraria = :grade")
    List<Horario> findByGradeHorariaForUpdate(@Param("grade") GradeHoraria grade);
}
