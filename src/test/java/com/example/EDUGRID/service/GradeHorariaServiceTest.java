package com.example.EduGrid.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.TamanhoGradeRepository;

class GradeHorariaServiceTest {

    @Test
    void carregaGradeMesmoComDuasAulasNaMesmaCelula() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Disciplina matematica = new Disciplina();
        matematica.setId(1);
        matematica.setNome("Matemática");
        Disciplina fisica = new Disciplina();
        fisica.setId(2);
        fisica.setNome("Física");
        Turma turma = new Turma();
        turma.setId(11);
        turma.setNome("Turma 1");
        turma.setDisciplinas(List.of(matematica, fisica));
        GradeHoraria grade = new GradeHoraria(List.of(turma), tamanhoGrade);
        grade.setId(84);
        List<Horario> horarios = List.of(
                new Horario(null, 0, 1, 2, turma, matematica, null),
                new Horario(null, 0, 1, 2, turma, fisica, null));
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        when(horarioRepository.findByGradeHoraria(grade)).thenReturn(horarios);
        GradeHorariaService service = new GradeHorariaService(
                mock(GradeHorariaRepository.class),
                mock(TamanhoGradeRepository.class),
                horarioRepository);

        assertDoesNotThrow(() -> service.carregarHorariosPersistidos(grade));
        grade.gerarMatriz();

        assertEquals("Matemática", grade.getMatriz()[0][11]);
    }

    @Test
    void associaHorariosLegadosApenasAViewMaisRecente() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Disciplina disciplina = new Disciplina();
        disciplina.setId(4);
        disciplina.setNome("Matemática");
        Turma turma = new Turma();
        turma.setId(11);
        turma.setNome("Turma 1");
        turma.setDisciplinas(List.of(disciplina));
        GradeHoraria grade = new GradeHoraria(List.of(turma), tamanhoGrade);
        grade.setId(83);
        Horario horario = new Horario(null, 0, 1, 2, turma, disciplina, null);
        GradeHorariaRepository gradeRepository = mock(GradeHorariaRepository.class);
        when(gradeRepository.findAllByOrderByIdDesc()).thenReturn(List.of(grade));
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        when(horarioRepository.findByGradeHoraria(grade)).thenReturn(List.of());
        when(horarioRepository.findByTurmaInAndGradeHorariaIsNull(List.of(turma)))
                .thenReturn(List.of(horario));
        GradeHorariaService service = new GradeHorariaService(
                gradeRepository,
                mock(TamanhoGradeRepository.class),
                horarioRepository
        );

        service.carregarHorariosPersistidos(grade);
        grade.gerarMatriz();

        assertEquals(grade, horario.getGradeHoraria());
        assertEquals("Matemática", grade.getMatriz()[0][11]);
        verify(horarioRepository).saveAll(anyIterable());
    }

    @Test
    void naoAssociaHorariosLegadosAUmaGradeAnterior() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Turma turma = new Turma();
        turma.setId(11);
        GradeHoraria gradeAnterior = new GradeHoraria(List.of(turma), tamanhoGrade);
        gradeAnterior.setId(82);
        GradeHoraria gradeMaisRecente = new GradeHoraria(List.of(), tamanhoGrade);
        gradeMaisRecente.setId(83);
        GradeHorariaRepository gradeRepository = mock(GradeHorariaRepository.class);
        when(gradeRepository.findAllByOrderByIdDesc())
                .thenReturn(List.of(gradeMaisRecente, gradeAnterior));
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        when(horarioRepository.findByGradeHoraria(gradeAnterior)).thenReturn(List.of());
        GradeHorariaService service = new GradeHorariaService(
                gradeRepository,
                mock(TamanhoGradeRepository.class),
                horarioRepository
        );

        service.carregarHorariosPersistidos(gradeAnterior);

        verify(horarioRepository, never()).findByTurmaInAndGradeHorariaIsNull(List.of(turma));
        verify(horarioRepository, never()).saveAll(anyIterable());
    }
}
