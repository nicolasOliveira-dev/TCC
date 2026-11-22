package com.example.EduGrid.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.TurmaRepository;

class TurmaServiceTest {

    @Test
    void atualizarTurmaAtualizaAno() {
        TurmaRepository turmaRepository = mock(TurmaRepository.class);
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        Turma turmaExistente = new Turma();
        Turma turmaAtualizada = new Turma();
        turmaAtualizada.setAno(2026);
        when(turmaRepository.findById(1)).thenReturn(Optional.of(turmaExistente));
        when(turmaRepository.save(turmaExistente)).thenReturn(turmaExistente);

        TurmaService service = new TurmaService(
                turmaRepository,
                horarioRepository,
                new BlocoDisciplinaService()
        );

        Turma resultado = service.atualizarTurma(1, turmaAtualizada);

        assertEquals(2026, resultado.getAno());
        verify(turmaRepository).save(turmaExistente);
    }

    @Test
    void alocarGradeHorariaMantemBlocosDefinidosEPeriodosAlocados() {
        TurmaRepository turmaRepository = mock(TurmaRepository.class);
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Disciplina disciplina = new Disciplina(
                1,
                "Disciplina de teste",
                "Geral",
                4.5,
                tamanhoGrade,
                null,
                "comum"
        );
        disciplina.setMinPeriodosJuntos(2);
        disciplina.setMaxPeriodosJuntos(4);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(disciplina),
                new Curso("Informática", "Técnico", "matutino"),
                tamanhoGrade
        );
        AtomicReference<List<Horario>> horariosSalvos = new AtomicReference<>();
        doAnswer(invocation -> {
            Iterable<Horario> horarios = invocation.getArgument(0);
            List<Horario> copia = StreamSupport.stream(horarios.spliterator(), false).toList();
            horariosSalvos.set(copia);
            return copia;
        }).when(horarioRepository).saveAll(anyIterable());

        TurmaService service = new TurmaService(
                turmaRepository,
                horarioRepository,
                new BlocoDisciplinaService()
        );
        service.alocarGradeHoraria(turma, tamanhoGrade, new ControleAlocacaoGlobal(5, 5));

        assertEquals(6, horariosSalvos.get().size());
        assertEquals(
                List.of(0, 1, 2, 3),
                horariosSalvos.get().stream()
                        .filter(horario -> horario.getDiaSemana() == 0)
                        .map(Horario::getPeriodoDia)
                        .sorted()
                        .toList()
        );
        assertEquals(
                List.of(0, 1),
                horariosSalvos.get().stream()
                        .filter(horario -> horario.getDiaSemana() == 1)
                        .map(Horario::getPeriodoDia)
                        .sorted()
                        .toList()
        );
    }
}
