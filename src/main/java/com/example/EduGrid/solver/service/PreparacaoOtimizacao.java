package com.example.EduGrid.solver.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;

import com.example.EduGrid.model.Professor;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;
import com.example.EduGrid.solver.domain.RegraHorarioEscolar;

public final class PreparacaoOtimizacao {
    private PreparacaoOtimizacao() { }

    public static void preparar(GradePlanejamento grade) {
        var erros = new LinkedHashSet<String>();
        if (grade.getAulas().isEmpty()) {
            throw new DiagnosticoOtimizacaoException("Nao ha aulas para otimizar. Confira as turmas e disciplinas cadastradas.");
        }
        for (var turma : grade.getTurmas()) {
            long carga = grade.getAulas().stream()
                    .filter(a -> Objects.equals(a.getTurma().getId(), turma.getId()))
                    .mapToLong(AulaPlanejamento::getDuracaoPeriodos).sum();
            int diasSemana = turma.getTamanhoGrade().getQtdeDiasSemana();
            int periodosDia = turma.getTamanhoGrade().getQtdePeriodosDia();
            int turno = turma.getCurso() == null ? -1
                    : HorarioPlanejamento.indiceDoTurno(turma.getCurso().getTurno());
            long capacidade = (long) diasSemana * periodosDia
                    - RegraHorarioEscolar.periodosIndisponiveisNaQuarta(
                            turno, diasSemana, periodosDia);
            if (carga > capacidade) {
                erros.add("Turma " + turma.getNome() + ": " + carga
                        + " periodos obrigatorios para apenas " + capacidade
                        + " vagas semanais permitidas pela configuracao e pelas restricoes.");
            }
        }
        for (var aula : grade.getAulas()) {
            String identificacao = aula.getTurma().getNome() + " / " + aula.getDisciplina().getNome();
            var professores = grade.getProfessores().stream()
                    .filter(p -> p.getDisciplinasPossiveis() != null
                            && p.getDisciplinasPossiveis().stream().anyMatch(d -> d != null
                            && Objects.equals(d.getId(), aula.getDisciplina().getId())))
                    .toList();
            int turno = HorarioPlanejamento.indiceDoTurno(aula.getTurma().getCurso().getTurno());
            int limite = aula.getTurma().getTamanhoGrade().getQtdePeriodosDia();
            var horarios = grade.getHorariosPossiveis().stream()
                    .filter(h -> h.getTurno() == turno && h.getPeriodoInicial() >= 0
                            && h.getPeriodoInicial() + aula.getDuracaoPeriodos() <= limite
                            && !RegraHorarioEscolar.ocupaPeriodoProibidoNaQuarta(
                                    h, aula.getDuracaoPeriodos(), limite))
                    .toList();
            if (professores.isEmpty()) {
                erros.add(identificacao + ": nenhum professor habilitado.");
            } else {
                // Filtragem estatica: nunca depende das variaveis escolhidas pelo solver.
                var horariosValidos = new ArrayList<HorarioPlanejamento>();
                for (var horario : horarios) {
                    if (professores.stream().anyMatch(p -> disponivel(p, horario, aula.getDuracaoPeriodos()))) {
                        horariosValidos.add(horario);
                    }
                }
                horarios = horariosValidos;
                var horariosFinais = horarios;
                professores = professores.stream().filter(p -> horariosFinais.stream()
                        .anyMatch(h -> disponivel(p, h, aula.getDuracaoPeriodos()))).toList();
                if (horarios.isEmpty()) {
                    erros.add(identificacao + ": nenhum professor habilitado possui disponibilidade para um bloco de "
                            + aula.getDuracaoPeriodos() + " periodos no turno da turma.");
                }
            }
            boolean horarioValido = horarios.contains(aula.getHorarioInicial());
            boolean professorValido = professores.contains(aula.getProfessor());
            if (aula.isBloqueadoManualmente() && (!horarioValido || !professorValido)) {
                erros.add(identificacao + ": a edição manual fixada não pode ser preservada, pois viola turno, disponibilidade ou habilitação docente.");
            } else {
                aula.setCandidatos(professores, horarios);
                if (!horarioValido) aula.setHorarioInicial(null);
                if (!professorValido) aula.setProfessor(null);
            }
        }
        if (!erros.isEmpty()) {
            throw new DiagnosticoOtimizacaoException("Confira os dados antes de otimizar:\n" + String.join("\n", erros));
        }
    }

    static boolean disponivel(Professor professor, HorarioPlanejamento horario, int duracao) {
        var matriz = professor.getDisponibilidadeHoraria();
        int turno = horario.getIndiceTurnoDisponibilidade();
        int dia = horario.getDiaSemana();
        if (matriz == null || turno < 0 || turno >= matriz.length || matriz[turno] == null
                || dia < 0 || duracao <= 0 || horario.getPeriodoInicial() < 0) {
            return false;
        }
        for (int p = horario.getPeriodoInicial(); p < horario.getPeriodoInicial() + duracao; p++) {
            if (p >= matriz[turno].length || matriz[turno][p] == null || dia >= matriz[turno][p].length
                    || !Boolean.TRUE.equals(matriz[turno][p][dia])) {
                return false;
            }
        }
        return true;
    }
}
