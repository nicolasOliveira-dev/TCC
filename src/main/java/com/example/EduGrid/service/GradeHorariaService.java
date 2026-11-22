package com.example.EduGrid.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.TamanhoGradeRepository;

@Service
public class GradeHorariaService {

    private final GradeHorariaRepository gradeHorariaRepository;
    private final TamanhoGradeRepository tamanhoGradeRepository;
    private final HorarioRepository horarioRepository;

    public GradeHorariaService(
            GradeHorariaRepository gradeHorariaRepository,
            TamanhoGradeRepository tamanhoGradeRepository,
            HorarioRepository horarioRepository) {

        this.gradeHorariaRepository = gradeHorariaRepository;
        this.tamanhoGradeRepository = tamanhoGradeRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional
    public void salvarGradeHoraria(GradeHoraria gradeHoraria) {

        tamanhoGradeRepository.save(gradeHoraria.getTamanhoGrade());

        gradeHorariaRepository.save(gradeHoraria);
    }

    @Transactional(readOnly = true)
    public List<GradeHoraria> listarGradesGeradas() {
        return gradeHorariaRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public GradeHoraria buscarGradeGerada(Integer id) {
        return gradeHorariaRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public GradeHoraria buscarGradeMaisRecente() {
        return gradeHorariaRepository.findFirstByOrderByIdDesc().orElse(null);
    }

    @Transactional
    public void carregarHorariosPersistidos(
            GradeHoraria gradeHoraria) {

        if (gradeHoraria == null
                || gradeHoraria.getTamanhoGrade() == null) {
            throw new IllegalArgumentException(
                    "A grade e sua configuração são obrigatórias."
            );
        }

        TamanhoGrade tamanhoGrade = gradeHoraria.getTamanhoGrade();
        List<Turma> turmas = gradeHoraria.getTurmas();

        if (turmas == null || turmas.isEmpty()) {
            return;
        }

        Map<Integer, Turma> turmasPorId = new HashMap<>();

        // Inicializar as matrizes das turmas exibidas.
        for (Turma turma : turmas) {

            if (turma == null || turma.getId() == null) {
                throw new IllegalStateException(
                        "A grade contém uma turma inválida."
                );
            }

            turma.setGradeHorariaMatriz(tamanhoGrade);

            if (turmasPorId.putIfAbsent(turma.getId(), turma) != null) {
                throw new IllegalStateException(
                        "A grade contém turmas duplicadas."
                );
            }
        }

        List<Horario> horarios;
        if (gradeHoraria.getId() == null) {
            horarios = horarioRepository.findByTurmaInAndGradeHorariaIsNull(turmas);
        } else {
            horarios = horarioRepository.findByGradeHoraria(gradeHoraria);
            if (horarios.isEmpty() && ehGradeMaisRecente(gradeHoraria)) {
                List<Horario> horariosLegados
                        = horarioRepository.findByTurmaInAndGradeHorariaIsNull(turmas);
                for (Horario horario : horariosLegados) {
                    horario.setGradeHoraria(gradeHoraria);
                }
                if (!horariosLegados.isEmpty()) {
                    horarioRepository.saveAll(horariosLegados);
                    horarios = horariosLegados;
                }
            }
        }

        for (Horario horario : horarios) {

            if (horario.getTurma() == null
                    || horario.getDisciplina() == null
                    || horario.getDisciplina().getId() == null) {
                throw new IllegalStateException(
                        "Existe um horário persistido incompleto."
                );
            }

            Turma turma = turmasPorId.get(
                    horario.getTurma().getId()
            );

            if (turma == null) {
                throw new IllegalStateException(
                        "Horário associado a uma turma fora da grade."
                );
            }

            int dia = horario.getDiaSemana();
            int periodo = horario.getPeriodoDia();

            if (dia < 0
                    || dia >= tamanhoGrade.getQtdeDiasSemana()
                    || periodo < 0
                    || periodo >= tamanhoGrade.getQtdePeriodosDia()) {

                throw new IllegalStateException(
                        "Horário persistido fora dos limites da grade."
                );
            }

            int[][] matriz = turma.getGradeHorariaMatriz();

            if (matriz[dia][periodo] != 0) {
                continue;
            }

            matriz[dia][periodo]
                    = horario.getDisciplina().getId();
        }
    }

    private boolean ehGradeMaisRecente(GradeHoraria gradeHoraria) {
        List<GradeHoraria> grades = gradeHorariaRepository.findAllByOrderByIdDesc();
        return !grades.isEmpty()
                && Objects.equals(grades.get(0).getId(), gradeHoraria.getId());
    }
}
