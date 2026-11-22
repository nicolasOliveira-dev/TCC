package com.example.EduGrid.solver.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;

@Service
public class PersistenciaGradeService {

    private final HorarioRepository horarioRepository;
    private final GradeHorariaRepository gradeHorariaRepository;

    public PersistenciaGradeService(
            HorarioRepository horarioRepository,
            GradeHorariaRepository gradeHorariaRepository) {
        this.horarioRepository = horarioRepository;
        this.gradeHorariaRepository = gradeHorariaRepository;
    }

    @Transactional
    public void persistirResultado(GradePlanejamento grade) {
        ResultadoValidado resultado = validarResultado(grade);

        for (Turma turma : resultado.turmas()) {
            horarioRepository.deleteByTurma(turma);
        }
        horarioRepository.saveAll(resultado.horarios());
    }

    @Transactional
    public GradeHoraria persistirResultado(GradePlanejamento grade, GradeHoraria gradeHoraria) {
        return persistirResultado(grade, gradeHoraria, "");
    }

    @Transactional
    public GradeHoraria persistirResultado(
            GradePlanejamento grade,
            GradeHoraria gradeHoraria,
            String relatorioRestricoesHard) {
        ResultadoValidado resultado = validarResultado(grade);
        if (gradeHoraria == null
                || gradeHoraria.getTamanhoGrade() == null
                || gradeHoraria.getTurmas() == null) {
            throw new IllegalArgumentException(
                    "A grade gerada precisa ter configuração e turmas."
            );
        }

        gradeHoraria.setRelatorioRestricoesHard(relatorioRestricoesHard);
        GradeHoraria gradeSalva = gradeHorariaRepository.save(gradeHoraria);
        gradeHoraria.setId(gradeSalva.getId());
        gradeHoraria.setDataGeracao(gradeSalva.getDataGeracao());
        for (Horario horario : resultado.horarios()) {
            horario.setGradeHoraria(gradeSalva);
        }
        horarioRepository.saveAll(resultado.horarios());
        return gradeSalva;
    }

    private ResultadoValidado validarResultado(GradePlanejamento grade) {
        if (grade == null || grade.getAulas() == null || grade.getAulas().isEmpty()) {
            throw new IllegalArgumentException("A grade de planejamento não pode estar vazia.");
        }
        if (grade.getScore() == null) {
            throw new IllegalStateException("A grade possui restrições obrigatórias não atendidas.");
        }
        if (grade.getTurmas() == null || grade.getTurmas().isEmpty()) {
            throw new IllegalStateException("A solução não contém as turmas que devem ser validadas.");
        }

        Map<Integer, Turma> turmasPorId = new HashMap<>();
        Map<ChaveTurmaDisciplina, Disciplina> disciplinasEsperadas = new HashMap<>();
        Map<ChaveTurmaDisciplina, Integer> periodosEsperados = new HashMap<>();
        for (Turma turma : grade.getTurmas()) {
            if (turma == null || turma.getId() == null) {
                throw new IllegalStateException("Todas as turmas devem possuir identificador.");
            }
            if (turmasPorId.putIfAbsent(turma.getId(), turma) != null) {
                throw new IllegalStateException("A solução contém turmas duplicadas: " + turma.getId());
            }
            if (turma.getDisciplinas() == null) {
                continue;
            }

            Set<Integer> idsDisciplinas = new HashSet<>();
            for (Disciplina disciplina : turma.getDisciplinas()) {
                if (disciplina == null || disciplina.getId() == null) {
                    throw new IllegalStateException(
                            "Todas as disciplinas das turmas devem possuir identificador.");
                }
                if (!idsDisciplinas.add(disciplina.getId())) {
                    throw new IllegalStateException(
                            "A turma contém disciplina duplicada: " + disciplina.getId());
                }

                int periodosObrigatorios = disciplina.getNumPeriodos();
                if (periodosObrigatorios < 0) {
                    throw new IllegalStateException(
                            "Carga horária inválida para a disciplina: " + disciplina.getNome());
                }
                ChaveTurmaDisciplina chave = new ChaveTurmaDisciplina(turma.getId(), disciplina.getId());
                disciplinasEsperadas.put(chave, disciplina);
                periodosEsperados.put(chave, periodosObrigatorios);
            }
        }

        Map<ChaveTurmaDisciplina, Integer> periodosNaSolucao = new HashMap<>();
        Set<String> idsAulas = new HashSet<>();
        List<Horario> horariosParaPersistir = new ArrayList<>();

        for (AulaPlanejamento aula : grade.getAulas()) {
            if (aula == null
                    || aula.getId() == null
                    || aula.getTurma() == null
                    || aula.getTurma().getId() == null
                    || aula.getDisciplina() == null
                    || aula.getDisciplina().getId() == null
                    || aula.getProfessor() == null
                    || aula.getHorarioInicial() == null) {
                throw new IllegalStateException("A grade contém aulas incompletas.");
            }
            if (!idsAulas.add(aula.getId())) {
                throw new IllegalStateException("A grade contém aula duplicada: " + aula.getId());
            }
            if (aula.getDuracaoPeriodos() <= 0) {
                throw new IllegalStateException("A duração do bloco deve ser positiva: " + aula.getId());
            }

            ChaveTurmaDisciplina chave = new ChaveTurmaDisciplina(
                    aula.getTurma().getId(),
                    aula.getDisciplina().getId()
            );
            Disciplina disciplinaEsperada = disciplinasEsperadas.get(chave);
            Turma turmaEsperada = turmasPorId.get(aula.getTurma().getId());
            if (disciplinaEsperada == null || turmaEsperada == null) {
                throw new IllegalStateException(
                        "A solução contém uma disciplina que não pertence à turma: " + aula.getId());
            }

            periodosNaSolucao.merge(chave, aula.getDuracaoPeriodos(), Math::addExact);
            validarHorario(aula, turmaEsperada);
            adicionarHorariosPersistidos(aula, turmaEsperada, disciplinaEsperada, horariosParaPersistir);
        }

        for (Map.Entry<ChaveTurmaDisciplina, Integer> esperado : periodosEsperados.entrySet()) {
            int periodoTotalNaSolucao = periodosNaSolucao.getOrDefault(esperado.getKey(), 0);
            if (periodoTotalNaSolucao != esperado.getValue()) {
                throw new IllegalStateException(
                        "Carga horária incompleta ou excedida para a disciplina "
                                + disciplinasEsperadas.get(esperado.getKey()).getNome()
                                + " da turma " + turmasPorId.get(esperado.getKey().turmaId()).getNome()
                                + ": esperado " + esperado.getValue()
                                + ", encontrado " + periodoTotalNaSolucao + ".");
            }
        }

        return new ResultadoValidado(new ArrayList<>(turmasPorId.values()), horariosParaPersistir);
    }

    private void validarHorario(
            AulaPlanejamento aula,
            Turma turma) {
        TamanhoGrade tamanhoGrade = turma.getTamanhoGrade();
        HorarioPlanejamento horario = aula.getHorarioInicial();
        if (tamanhoGrade == null
                || horario.getDiaSemana() < 0
                || horario.getDiaSemana() >= tamanhoGrade.getQtdeDiasSemana()
                || horario.getPeriodoInicial() < 0
                || horario.getPeriodoInicial() + aula.getDuracaoPeriodos()
                        > tamanhoGrade.getQtdePeriodosDia()) {
            throw new IllegalStateException("Horário inválido para a aula: " + aula.getId());
        }

        if (horario.getTurno() < 0 || horario.getTurno() > 2) {
            throw new IllegalStateException("Turno fora dos limites da grade para a aula: " + aula.getId());
        }
    }

    private void adicionarHorariosPersistidos(
            AulaPlanejamento aula,
            Turma turma,
            Disciplina disciplina,
            List<Horario> horariosParaPersistir) {
        HorarioPlanejamento horario = aula.getHorarioInicial();
        for (int indice = 0; indice < aula.getDuracaoPeriodos(); indice++) {
            Horario registro = new Horario(
                    null,
                    horario.getTurno(),
                    horario.getPeriodoInicial() + indice,
                    horario.getDiaSemana(),
                    turma,
                    disciplina,
                    aula.getProfessor()
            );
            registro.setManualmenteEditado(aula.isBloqueadoManualmente());
            horariosParaPersistir.add(registro);
        }
    }

    private record ChaveTurmaDisciplina(Integer turmaId, Integer disciplinaId) {
    }

    private record ResultadoValidado(List<Turma> turmas, List<Horario> horarios) {
    }
}
