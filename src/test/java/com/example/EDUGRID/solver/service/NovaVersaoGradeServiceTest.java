package com.example.EduGrid.solver.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.*;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;

class NovaVersaoGradeServiceTest {
    @Test
    void doesNotPersistCandidateUntilUserAppliesAnImprovedFeasibleResult() throws Exception {
        Fixture f = fixture(true);
        UUID tentativa = f.service.iniciar(1, false);
        aguardar(f.service, tentativa);

        NovaVersaoGradeService.Resumo resumo = f.service.resumo(tentativa);
        assertEquals("0hard/0soft", resumo.pontuacaoCandidata());
        assertTrue(resumo.viavel());
        assertTrue(resumo.melhorou());
        verify(f.persistencia, never()).persistirResultado(any(), any(GradeHoraria.class));
        assertEquals(2, f.horariosOrigem.size());
        assertEquals(List.of(0, 0), f.horariosOrigem.stream().map(Horario::getPeriodoDia).toList());

        Integer novaGradeId = f.service.aplicar(tentativa);

        assertEquals(2, novaGradeId);
        ArgumentCaptor<GradeHoraria> gradeAplicada = ArgumentCaptor.forClass(GradeHoraria.class);
        verify(f.persistencia).persistirResultado(eq(f.candidata), gradeAplicada.capture());
        assertNotSame(f.gradeOrigem.getTurmas(), gradeAplicada.getValue().getTurmas());
        assertEquals(f.gradeOrigem.getTurmas(), gradeAplicada.getValue().getTurmas());
        assertEquals(List.of(0, 0), f.horariosOrigem.stream().map(Horario::getPeriodoDia).toList());
    }

    @Test
    void refusesCandidateThatDoesNotBeatTheCurrentScore() throws Exception {
        Fixture f = fixture(false);
        UUID tentativa = f.service.iniciar(1, false);
        aguardar(f.service, tentativa);

        NovaVersaoGradeService.Resumo resumo = f.service.resumo(tentativa);
        assertTrue(resumo.viavel());
        assertFalse(resumo.melhorou());
        assertThrows(IllegalStateException.class, () -> f.service.aplicar(tentativa));
        verify(f.persistencia, never()).persistirResultado(any(), any(GradeHoraria.class));
    }

    private Fixture fixture(boolean candidateIsImproved) throws Exception {
        TamanhoGrade size = new TamanhoGrade(3, 5, 5);
        Curso course = new Curso("Curso", "Técnico", "matutino");
        Turma turma = new Turma(1, "Turma A", List.of(), course, size, 1);
        Disciplina first = disciplina(1, "A");
        Disciplina second = disciplina(2, "B");
        turma.setDisciplinas(List.of(first, second));
        Professor professorA = professor(1, first, second);
        Professor professorB = professor(2, first, second);
        HorarioPlanejamento slot0 = new HorarioPlanejamento(0, 0, 0, 0);
        HorarioPlanejamento slot1 = new HorarioPlanejamento(0, 1, 0, 0);
        AulaPlanejamento a = new AulaPlanejamento("a", turma, first, 1, professorA, slot0);
        HorarioPlanejamento originalSecondSlot = candidateIsImproved ? slot0 : slot1;
        AulaPlanejamento b = new AulaPlanejamento("b", turma, second, 1, professorB, originalSecondSlot);
        GradePlanejamento original = new GradePlanejamento(List.of(a, b), List.of(slot0, slot1),
                List.of(professorA, professorB), null, List.of());
        original.setTurmas(List.of(turma));
        GradePlanejamento candidata = new GradePlanejamento(
                List.of(copy(a, slot0), copy(b, slot1)),
                List.of(slot0, slot1), List.of(professorA, professorB), null, List.of());
        candidata.setTurmas(List.of(turma));

        GradeHoraria source = new GradeHoraria(List.of(turma), size); source.setId(1);
        Horario rowA = row(1, turma, first, professorA, source, 0);
        Horario rowB = row(2, turma, second, professorB, source, candidateIsImproved ? 0 : 1);
        List<Horario> sourceRows = List.of(rowA, rowB);
        GradeHorariaRepository grades = mock(GradeHorariaRepository.class);
        when(grades.findById(1)).thenReturn(Optional.of(source));
        when(grades.save(any(GradeHoraria.class))).thenAnswer(invocation -> {
            GradeHoraria saved = invocation.getArgument(0); saved.setId(2); return saved;
        });
        HorarioRepository schedule = mock(HorarioRepository.class);
        when(schedule.findByGradeHoraria(source)).thenReturn(sourceRows);
        OtimizacaoService optimizer = mock(OtimizacaoService.class);
        when(optimizer.montarProblema(eq(List.of(turma)), eq(size), eq(source), eq(false))).thenReturn(original);
        SolverJob<GradePlanejamento> solverJob = mock(SolverJob.class);
        when(solverJob.getFinalBestSolution()).thenReturn(candidata);
        SolverManager<GradePlanejamento> manager = mock(SolverManager.class);
        when(optimizer.iniciarNovaTentativa(eq(original), any(UUID.class)))
                .thenAnswer(invocation -> new OtimizacaoService.NovaTentativa(
                        solverJob, manager, 234L));
        PersistenciaGradeService persistence = mock(PersistenciaGradeService.class);
        when(persistence.persistirResultado(eq(candidata), any(GradeHoraria.class))).thenAnswer(invocation -> {
            GradeHoraria saved = invocation.getArgument(1); saved.setId(2); return saved;
        });
        return new Fixture(new NovaVersaoGradeService(grades, schedule, optimizer, persistence),
                persistence, sourceRows, candidata, source);
    }

    private void aguardar(NovaVersaoGradeService service, UUID id) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while ("PROCESSANDO".equals(service.status(id)) && System.nanoTime() < limite) Thread.sleep(10);
        assertEquals("CONCLUIDO", service.status(id));
    }

    private AulaPlanejamento copy(AulaPlanejamento source, HorarioPlanejamento slot) {
        return new AulaPlanejamento(source.getId(), source.getTurma(), source.getDisciplina(),
                source.getDuracaoPeriodos(), source.getProfessor(), slot);
    }

    private Disciplina disciplina(int id, String name) {
        Disciplina d = new Disciplina(); d.setId(id); d.setNome(name); d.setTipoDisciplina("comum");
        d.setMinPeriodosJuntos(1); d.setMaxPeriodosJuntos(2); return d;
    }

    private Professor professor(int id, Disciplina... disciplines) {
        Boolean[][][] availability = new Boolean[3][5][5];
        for (Boolean[][] turn : availability) for (Boolean[] period : turn) java.util.Arrays.fill(period, true);
        Professor p = new Professor(); p.setId(id); p.setNome("Docente " + id);
        p.setDisponibilidadeHoraria(availability); p.setDisciplinasPossiveis(List.of(disciplines)); return p;
    }

    private Horario row(int id, Turma turma, Disciplina d, Professor p, GradeHoraria g, int period) {
        return new Horario(id, 0, period, 0, turma, d, p) {{ setGradeHoraria(g); }};
    }

    private record Fixture(NovaVersaoGradeService service, PersistenciaGradeService persistencia,
            List<Horario> horariosOrigem, GradePlanejamento candidata, GradeHoraria gradeOrigem) {}
}
