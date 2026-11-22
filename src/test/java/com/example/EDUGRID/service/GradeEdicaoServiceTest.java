package com.example.EduGrid.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.*;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;

class GradeEdicaoServiceTest {
    @Test
    void previewMoveBlockDoesNotPersistAndPreservesRows() {
        Fixture f = fixture(2, 2, false);
        GradeEdicaoService.Resultado result = f.service.mover(1,
                new GradeEdicaoService.Movimento(1, 0, 0, 1, 0, false), false);
        assertTrue(result.valido());
        assertFalse(result.salvo());
        assertEquals(List.of(0, 1), f.hours.stream().map(Horario::getDiaSemana).distinct().toList());
        verify(f.horarioRepository, never()).saveAll(anyIterable());
    }

    @Test
    void swapsEqualDurationBlocksAndPersistsWhenConfirmed() {
        Fixture f = fixture(2, 2, false);
        GradeEdicaoService.Resultado result = f.service.mover(1,
                new GradeEdicaoService.Movimento(1, 0, 0, 1, 0, false), true);
        assertTrue(result.valido());
        assertTrue(result.salvo());
        assertEquals(List.of(1, 1), f.hours.subList(0, 2).stream().map(Horario::getDiaSemana).toList());
        assertEquals(List.of(0, 0), f.hours.subList(2, 4).stream().map(Horario::getDiaSemana).toList());
        assertTrue(f.hours.stream().allMatch(Horario::isManualmenteEditado));
        verify(f.horarioRepository).saveAll(f.hours);
    }

    @Test
    void swapsDifferentDurationBlocksWhenBothFitInTheirNewPositions() {
        Fixture f = fixture(2, 1, false);
        GradeEdicaoService.Resultado result = f.service.mover(1,
                new GradeEdicaoService.Movimento(1, 0, 0, 1, 0, false), true);
        assertTrue(result.valido());
        assertTrue(result.salvo());
        assertEquals(1, f.hours.get(0).getDiaSemana());
        assertEquals(1, f.hours.get(1).getDiaSemana());
        assertEquals(0, f.hours.get(2).getDiaSemana());
        verify(f.horarioRepository).saveAll(f.hours);
    }

    @Test
    void savesUnavailableAndWednesdayForbiddenTargetsAndReportsViolations() {
        Fixture indisponivel = fixture(2, 2, true);
        GradeEdicaoService.Resultado a = indisponivel.service.mover(1,
                new GradeEdicaoService.Movimento(1, 0, 0, 1, 0, false), true);
        assertTrue(a.valido());
        assertTrue(a.salvo());
        assertTrue(a.restricoesHard().stream().anyMatch(s -> s.contains("indisponível")));
        Fixture quarta = fixture(2, 2, false);
        GradeEdicaoService.Resultado b = quarta.service.mover(1,
                new GradeEdicaoService.Movimento(1, 0, 0, 2, 3, false), true);
        assertTrue(b.valido());
        assertTrue(b.salvo());
        assertTrue(b.restricoesHard().stream().anyMatch(s -> s.contains("quarta-feira")));
    }

    @Test
    void savesWhenCourseTurnIsUnknownAndReportsTheMismatch() {
        Fixture f = fixture(1, 1, false);
        f.grade.getTurmas().get(0).getCurso().setTurno("Turno legado");

        GradeEdicaoService.Resultado result = f.service.mover(1,
                new GradeEdicaoService.Movimento(1, 0, 0, 1, 0, false), true);

        assertTrue(result.valido());
        assertTrue(result.salvo());
        assertTrue(result.restricoesHard().stream().anyMatch(s -> s.contains("Turno incompat")));
        assertTrue(f.grade.getRelatorioEdicao().contains("Turno incompat"));
    }

    @Test
    void savesSoftWarningsWithoutAskingForConfirmation() {
        Fixture f = fixture(2, 2, false);
        f.professor.setIntencaoDiaPlanejamento(List.of("Terça-feira"));
        GradeEdicaoService.Resultado result = f.service.mover(1,
                new GradeEdicaoService.Movimento(1, 0, 0, 1, 0, false), true);
        assertTrue(result.valido());
        assertTrue(result.salvo());
        assertFalse(result.restricoesSoft().isEmpty());
        assertTrue(f.grade.getRelatorioEdicao().contains("Resultado da edi"));
        verify(f.horarioRepository).saveAll(f.hours);
    }

    private Fixture fixture(int firstDuration, int secondDuration, boolean unavailableTarget) {
        TamanhoGrade size = new TamanhoGrade(3, 5, 5);
        Curso course = new Curso("Curso", "Técnico", "Matutino");
        Turma turma = new Turma(); turma.setId(1); turma.setNome("Turma A"); turma.setCurso(course);
        Professor professor = new Professor(); professor.setId(1); professor.setNome("Docente");
        Boolean[][][] availability = new Boolean[3][5][5];
        for (int t=0;t<3;t++) for (int p=0;p<5;p++) for (int d=0;d<5;d++) availability[t][p][d] = true;
        if (unavailableTarget) availability[0][0][1] = false;
        professor.setDisponibilidadeHoraria(availability);
        Disciplina a = new Disciplina(); a.setId(1); a.setNome("A");
        Disciplina b = new Disciplina(); b.setId(2); b.setNome("B");
        professor.setDisciplinasPossiveis(List.of(a, b));
        GradeHoraria grade = new GradeHoraria(new ArrayList<>(List.of(turma)), size); grade.setId(1);
        List<Horario> rows = new ArrayList<>();
        for (int p=0;p<firstDuration;p++) rows.add(h(1+p, 0, p, turma, a, professor, grade));
        for (int p=0;p<secondDuration;p++) rows.add(h(10+p, 1, p, turma, b, professor, grade));
        GradeHorariaRepository grades = mock(GradeHorariaRepository.class);
        when(grades.findById(1)).thenReturn(Optional.of(grade));
        HorarioRepository repository = mock(HorarioRepository.class);
        when(repository.findByGradeHorariaForUpdate(grade)).thenReturn(rows);
        return new Fixture(new GradeEdicaoService(grades, repository), repository, rows, professor, grade);
    }

    private Horario h(int id, int day, int period, Turma t, Disciplina d, Professor p, GradeHoraria g) {
        Horario h = new Horario(); h.setId(id); h.setDiaSemana(day); h.setPeriodoDia(period); h.setTurno(0);
        h.setTurma(t); h.setDisciplina(d); h.setProfessor(p); h.setGradeHoraria(g); return h;
    }

    private record Fixture(GradeEdicaoService service, HorarioRepository horarioRepository,
            List<Horario> hours, Professor professor, GradeHoraria grade) {}
}
