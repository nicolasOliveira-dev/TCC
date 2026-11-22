package com.example.EduGrid.solver.constraint;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;
import com.example.EduGrid.solver.domain.RegraHorarioEscolar;
import com.example.EduGrid.solver.domain.VinculoSincronizacao;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

public class GradeConstraintProvider implements ConstraintProvider {

    private String diagnosticoNome;

    /**
     * Usado apenas no recalculo diagnostico de uma regra isolada.
     */
    public void setDiagnosticoNome(String nome) {
        diagnosticoNome = nome;
    }

    private static final List<String> DIAS_DA_SEMANA = List.of(
            "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
            "Sexta-feira", "Sábado", "Domingo");

    @Override
    public Constraint[] defineConstraints(
            ConstraintFactory constraintFactory) {

        Constraint[] restricoes = new Constraint[]{
            conflitoProfessor(constraintFactory),
            conflitoTurma(constraintFactory),
            limitePeriodosTurno(constraintFactory),
            turnoIncompativel(constraintFactory),
            ultimosPeriodosQuartaFeira(constraintFactory),
            professorNaoHabilitado(constraintFactory),
            professorIndisponivel(constraintFactory),
            professorSemDiaPlanejamento(constraintFactory),
            preferenciaDiaPlanejamento(constraintFactory),
            jornadaMaximaProfessor(constraintFactory),
            descansoEntreTurnos(constraintFactory),
            disciplinasConsecutivasProfessor(constraintFactory),
            mesmaDisciplinaTecnicaMesmoAno(constraintFactory),
            sincronizacaoDisciplinasTecnicas(constraintFactory),
            sincronizacaoNivelamento(constraintFactory),
            sincronizacaoEletivas(constraintFactory)
        };
        if (diagnosticoNome == null) {
            return restricoes;
        }
        Constraint[] selecionadas = java.util.Arrays.stream(restricoes)
                .filter(c -> c.getConstraintRef().id().equals(diagnosticoNome))
                .toArray(Constraint[]::new);
        if (selecionadas.length != 1) {
            throw new IllegalArgumentException("Restricao de diagnostico desconhecida: " + diagnosticoNome);
        }
        return selecionadas;
    }

    boolean horariosSobrepostos(
            AulaPlanejamento aula1,
            AulaPlanejamento aula2) {

        HorarioPlanejamento h1 = aula1.getHorarioInicial();
        HorarioPlanejamento h2 = aula2.getHorarioInicial();

        if (h1 == null || h2 == null) {
            return false;
        }

        if (h1.getDiaSemana() != h2.getDiaSemana()
                || h1.getTurno() != h2.getTurno()) {
            return false;
        }

        int inicio1 = h1.getPeriodoInicial();
        int fim1 = inicio1 + aula1.getDuracaoPeriodos();

        int inicio2 = h2.getPeriodoInicial();
        int fim2 = inicio2 + aula2.getDuracaoPeriodos();

        return inicio1 < fim2 && inicio2 < fim1;
    }

    Constraint conflitoProfessor(ConstraintFactory factory) {
        return factory
                .forEachUniquePair(
                        AulaPlanejamento.class,
                        Joiners.equal(AulaPlanejamento::getProfessor))
                .filter(this::horariosSobrepostos)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflito de professor");
    }

    Constraint conflitoTurma(ConstraintFactory factory) {
        return factory
                .forEachUniquePair(
                        AulaPlanejamento.class,
                        Joiners.equal(aula -> aula.getTurma().getId()))
                .filter(this::horariosSobrepostos)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflito de turma");
    }

    Constraint limitePeriodosTurno(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> {
                    HorarioPlanejamento horario = aula.getHorarioInicial();
                    if (aula.getTurma() == null
                            || aula.getTurma().getTamanhoGrade() == null
                            || horario == null) {
                        return true;
                    }

                    int periodoInicial = horario.getPeriodoInicial();
                    int duracao = aula.getDuracaoPeriodos();
                    int limite = aula.getTurma().getTamanhoGrade().getQtdePeriodosDia();

                    return periodoInicial < 0
                            || duracao <= 0
                            || periodoInicial + duracao > limite;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Bloco ultrapassa limite do turno");
    }

    Constraint turnoIncompativel(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> {
                    if (aula.getTurma() == null
                            || aula.getTurma().getCurso() == null
                            || aula.getTurma().getCurso().getTurno() == null
                            || aula.getHorarioInicial() == null) {
                        return true;
                    }

                    int turnoEsperado = HorarioPlanejamento.indiceDoTurno(
                            aula.getTurma().getCurso().getTurno()
                    );
                    return aula.getHorarioInicial().getTurno() != turnoEsperado;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Turno incompativel com a turma");
    }

    Constraint ultimosPeriodosQuartaFeira(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> aula.getTurma() != null
                        && aula.getTurma().getTamanhoGrade() != null
                        && RegraHorarioEscolar.ocupaPeriodoProibidoNaQuarta(
                                aula.getHorarioInicial(),
                                aula.getDuracaoPeriodos(),
                                aula.getTurma().getTamanhoGrade().getQtdePeriodosDia()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Aulas nos dois ultimos periodos da quarta-feira");
    }

    Constraint professorNaoHabilitado(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> {
                    if (aula.getProfessor() == null
                            || aula.getDisciplina() == null) {
                        return true;
                    }
                    return aula.getProfessor().getDisciplinasPossiveis() == null
                            || aula.getProfessor().getDisciplinasPossiveis().stream()
                                    .filter(disciplina -> disciplina != null)
                                    .noneMatch(disciplina -> Objects.equals(
                                            disciplina.getId(),
                                            aula.getDisciplina().getId()
                                    ));
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Professor nao habilitado para a disciplina");
    }

    Constraint professorIndisponivel(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> {
                    var horario = aula.getHorarioInicial();

                    if (aula.getProfessor() == null || horario == null) {
                        return true;
                    }

                    Boolean[][][] disponibilidade
                            = aula.getProfessor().getDisponibilidadeHoraria();

                    int turno = horario.getIndiceTurnoDisponibilidade();
                    int dia = horario.getDiaSemana();
                    int inicio = horario.getPeriodoInicial();
                    int duracao = aula.getDuracaoPeriodos();

                    if (disponibilidade == null
                            || turno < 0
                            || turno >= disponibilidade.length
                            || disponibilidade[turno] == null
                            || inicio < 0
                            || dia < 0
                            || duracao <= 0) {
                        return true;
                    }

                    for (int periodo = inicio;
                            periodo < inicio + duracao;
                            periodo++) {

                        if (periodo >= disponibilidade[turno].length
                                || disponibilidade[turno][periodo] == null
                                || dia >= disponibilidade[turno][periodo].length
                                || !Boolean.TRUE.equals(
                                        disponibilidade[turno][periodo][dia])) {
                            return true;
                        }
                    }

                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Professor indisponivel no horario");
    }

    Constraint professorSemDiaPlanejamento(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> aula != null
                        && aula.getProfessor() != null
                        && aula.getHorarioInicial() != null)
                .groupBy(
                        AulaPlanejamento::getProfessor,
                        ConstraintCollectors.<AulaPlanejamento, Integer>toSet(
                                aula -> aula.getHorarioInicial().getDiaSemana()
                        ),
                        ConstraintCollectors.<AulaPlanejamento, Integer>max(
                                aula -> aula.getTurma() == null
                                || aula.getTurma().getTamanhoGrade() == null
                                ? 0
                                : aula.getTurma().getTamanhoGrade().getQtdeDiasSemana()
                        )
                )
                .filter((professor, diasOcupados, totalDias)
                        -> totalDias > 0 && diasOcupados.size() >= totalDias)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Professor sem dia de planejamento");
    }

    Constraint preferenciaDiaPlanejamento(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> {
                    var horario = aula.getHorarioInicial();

                    if (aula.getProfessor() == null || horario == null) {
                        return false;
                    }

                    List<String> diasPreferencia
                            = aula.getProfessor().getIntencaoDiaPlanejamento();

                    int dia = horario.getDiaSemana();

                    return diasPreferencia != null
                            && dia >= 0
                            && dia < DIAS_DA_SEMANA.size()
                            && diasPreferencia.contains(
                                    DIAS_DA_SEMANA.get(dia));
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Aula no dia de planejamento preferido");
    }

    Constraint jornadaMaximaProfessor(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> aula != null
                        && aula.getProfessor() != null
                        && aula.getHorarioInicial() != null)
                .groupBy(
                        AulaPlanejamento::getProfessor,
                        aula -> aula.getHorarioInicial().getDiaSemana(),
                        aula -> aula.getHorarioInicial().getTurno(),
                        ConstraintCollectors.toList()
                )
                .filter((professor, dia, turno, aulas)
                        -> excedeJornadaMaxima(
                                aulas,
                                professor.getJornadaMaxima()
                        )
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Professor excedeu jornada maxima");
    }

    private boolean excedeJornadaMaxima(
            List<AulaPlanejamento> aulas,
            int jornadaMaxima) {

        if (aulas == null || aulas.isEmpty() || jornadaMaxima < 0) {
            return false;
        }

        List<int[]> intervalos = aulas.stream()
                .filter(aula -> aula != null
                        && aula.getHorarioInicial() != null
                        && aula.getDuracaoPeriodos() > 0)
                .map(aula -> {
                    int inicio = aula.getHorarioInicial().getPeriodoInicial();
                    int fim = inicio + aula.getDuracaoPeriodos();
                    return new int[]{inicio, fim};
                })
                .sorted(Comparator.comparingInt(intervalo -> intervalo[0]))
                .toList();

        if (intervalos.isEmpty()) {
            return false;
        }

        int inicioSequencia = intervalos.get(0)[0];
        int fimSequencia = intervalos.get(0)[1];

        for (int i = 1; i < intervalos.size(); i++) {
            int[] intervalo = intervalos.get(i);
            int inicio = intervalo[0];
            int fim = intervalo[1];

            if (inicio <= fimSequencia) {
                fimSequencia = Math.max(fimSequencia, fim);
            } else {
                if (fimSequencia - inicioSequencia > jornadaMaxima) {
                    return true;
                }
                inicioSequencia = inicio;
                fimSequencia = fim;
            }
        }

        return fimSequencia - inicioSequencia > jornadaMaxima;
    }

    Constraint descansoEntreTurnos(ConstraintFactory factory) {
        return factory
                .forEachUniquePair(
                        AulaPlanejamento.class,
                        Joiners.equal(AulaPlanejamento::getProfessor)
                )
                .filter((aula1, aula2) -> {
                    HorarioPlanejamento h1 = aula1.getHorarioInicial();
                    HorarioPlanejamento h2 = aula2.getHorarioInicial();

                    if (h1 == null || h2 == null) {
                        return false;
                    }

                    return (h1.getTurno() == 2
                            && h2.getTurno() == 0
                            && h2.getDiaSemana() == h1.getDiaSemana() + 1) || (h2.getTurno() == 2
                            && h1.getTurno() == 0
                            && h1.getDiaSemana() == h2.getDiaSemana() + 1);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Descanso entre turnos");
    }

    Constraint disciplinasConsecutivasProfessor(ConstraintFactory factory) {
        return factory
                .forEachUniquePair(
                        AulaPlanejamento.class,
                        Joiners.equal(AulaPlanejamento::getProfessor)
                )
                .filter((aula1, aula2) -> {
                    HorarioPlanejamento h1 = aula1.getHorarioInicial();
                    HorarioPlanejamento h2 = aula2.getHorarioInicial();

                    if (h1 == null || h2 == null) {
                        return false;
                    }

                    if (h1.getDiaSemana() != h2.getDiaSemana()
                            || h1.getTurno() != h2.getTurno()) {
                        return false;
                    }

                    if (aula1.getDisciplina() == null
                            || aula2.getDisciplina() == null
                            || aula1.getDisciplina().getId() == null
                            || aula2.getDisciplina().getId() == null) {
                        return false;
                    }

                    if (aula1.getDisciplina().getId()
                            .equals(aula2.getDisciplina().getId())) {
                        return false;
                    }

                    int fim1 = h1.getPeriodoInicial()
                            + aula1.getDuracaoPeriodos();

                    int fim2 = h2.getPeriodoInicial()
                            + aula2.getDuracaoPeriodos();

                    return fim1 == h2.getPeriodoInicial()
                            || fim2 == h1.getPeriodoInicial();
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Disciplinas consecutivas do mesmo professor");
    }

    Constraint mesmaDisciplinaTecnicaMesmoAno(
            ConstraintFactory factory) {

        return factory
                .forEachUniquePair(
                        AulaPlanejamento.class,
                        Joiners.equal(
                                aula -> aula.getTurma()
                                        .getCurso().getId()
                        ),
                        Joiners.equal(
                                aula -> aula.getTurma().getAno()
                        ),
                        Joiners.equal(
                                aula -> aula.getDisciplina().getId()
                        )
                )
                .filter((aula1, aula2) -> {

                    if (aula1.getTurma().getId()
                            .equals(aula2.getTurma().getId())) {
                        return false;
                    }

                    if (!"tecnica".equalsIgnoreCase(
                            aula1.getDisciplina()
                                    .getTipoDisciplina())) {
                        return false;
                    }

                    return horariosSobrepostos(aula1, aula2);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint(
                        "Mesma disciplina tecnica simultanea no mesmo ano"
                );
    }

    Constraint sincronizacaoDisciplinasTecnicas(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .join(
                        VinculoSincronizacao.class,
                        Joiners.equal(AulaPlanejamento::getId, VinculoSincronizacao::aulaId)
                )
                .groupBy(
                        (aula, vinculo) -> vinculo.grupoId(),
                        ConstraintCollectors.toList(
                                (aula, vinculo) -> new AulaVinculada(vinculo.turmaId(), aula)
                        )
                )
                .filter((grupoId, aulas) -> grupoId != null && aulas != null && !aulas.isEmpty())
                .penalize(
                        HardSoftScore.ONE_SOFT,
                        (grupoId, aulas) -> 10 * calcularPeriodosNaoSincronizados(aulas)
                )
                .asConstraint(
                        "Sincronizacao de disciplinas tecnicas por grupo"
                );
    }

    Constraint sincronizacaoNivelamento(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> ehTipoDisciplina(aula, "nivelamento")
                        && podeAgruparSincronizacao(aula, true))
                .groupBy(
                        aula -> chaveSincronizacao(aula, true),
                        ConstraintCollectors.toList()
                )
                .filter((chave, aulas) -> contarTurmas(aulas) > 1)
                .penalize(
                        HardSoftScore.ONE_HARD,
                        (chave, aulas) -> calcularPeriodosDivergentes(aulas)
                )
                .asConstraint("Sincronizacao de nivelamento por disciplina ano e turno");
    }

    Constraint sincronizacaoEletivas(ConstraintFactory factory) {
        return factory
                .forEach(AulaPlanejamento.class)
                .filter(aula -> ehTipoDisciplina(aula, "eletiva")
                        && podeAgruparSincronizacao(aula, false))
                .groupBy(
                        aula -> chaveSincronizacao(aula, false),
                        ConstraintCollectors.toList()
                )
                .filter((chave, aulas) -> contarTurmas(aulas) > 1)
                .penalize(
                        HardSoftScore.ONE_HARD,
                        (chave, aulas) -> calcularPeriodosDivergentes(aulas)
                )
                .asConstraint("Sincronizacao de eletivas por disciplina e turno");
    }

    private boolean ehTipoDisciplina(AulaPlanejamento aula, String tipo) {
        return aula != null
                && aula.getDisciplina() != null
                && aula.getDisciplina().getTipoDisciplina() != null
                && tipo.equalsIgnoreCase(aula.getDisciplina().getTipoDisciplina().trim());
    }

    private boolean podeAgruparSincronizacao(AulaPlanejamento aula, boolean exigeAno) {
        return aula.getDisciplina().getId() != null
                && aula.getTurma() != null
                && aula.getTurma().getId() != null
                && aula.getTurma().getCurso() != null
                && aula.getTurma().getCurso().getTurno() != null
                && !aula.getTurma().getCurso().getTurno().isBlank()
                && (!exigeAno || aula.getTurma().getAno() != null);
    }

    private ChaveSincronizacao chaveSincronizacao(AulaPlanejamento aula, boolean incluiAno) {
        String turno = aula.getTurma().getCurso().getTurno()
                .trim()
                .toLowerCase(Locale.ROOT);
        Integer ano = incluiAno ? aula.getTurma().getAno() : null;
        return new ChaveSincronizacao(aula.getDisciplina().getId(), ano, turno);
    }

    private int contarTurmas(List<AulaPlanejamento> aulas) {
        return (int) aulas.stream()
                .map(AulaPlanejamento::getTurma)
                .filter(turma -> turma != null && turma.getId() != null)
                .map(turma -> turma.getId())
                .distinct()
                .count();
    }

    private int calcularPeriodosDivergentes(List<AulaPlanejamento> aulas) {
        Map<Integer, Set<PeriodoOcupado>> periodosPorTurma = new HashMap<>();
        for (AulaPlanejamento aula : aulas) {
            if (aula == null || aula.getTurma() == null || aula.getTurma().getId() == null) {
                continue;
            }

            Set<PeriodoOcupado> periodos = periodosPorTurma.computeIfAbsent(
                    aula.getTurma().getId(),
                    ignorado -> new HashSet<>()
            );
            HorarioPlanejamento horario = aula.getHorarioInicial();
            if (horario == null || aula.getDuracaoPeriodos() <= 0) {
                continue;
            }

            for (int periodo = horario.getPeriodoInicial();
                    periodo < horario.getPeriodoInicial() + aula.getDuracaoPeriodos();
                    periodo++) {
                periodos.add(new PeriodoOcupado(
                        horario.getDiaSemana(),
                        horario.getTurno(),
                        periodo
                ));
            }
        }

        if (periodosPorTurma.size() < 2) {
            return 0;
        }

        Set<PeriodoOcupado> todosPeriodos = new HashSet<>();
        periodosPorTurma.values().forEach(todosPeriodos::addAll);

        int divergencias = 0;
        int quantidadeTurmas = periodosPorTurma.size();
        for (PeriodoOcupado periodo : todosPeriodos) {
            int turmasComPeriodo = (int) periodosPorTurma.values().stream()
                    .filter(periodos -> periodos.contains(periodo))
                    .count();
            divergencias += Math.min(turmasComPeriodo, quantidadeTurmas - turmasComPeriodo);
        }
        return divergencias;
    }

    private record ChaveSincronizacao(Integer disciplinaId, Integer ano, String turno) {

    }

    private int calcularPeriodosNaoSincronizados(List<AulaVinculada> aulasVinculadas) {
        Map<Integer, Set<PeriodoOcupado>> periodosPorTurma = new HashMap<>();
        Map<Integer, Set<String>> aulasContabilizadasPorTurma = new HashMap<>();
        Map<Integer, Integer> duracaoPorTurma = new HashMap<>();

        for (AulaVinculada vinculada : aulasVinculadas) {
            AulaPlanejamento aula = vinculada.aula();
            Integer turmaId = vinculada.turmaId();
            if (aula == null || turmaId == null) {
                continue;
            }

            String aulaId = aula.getId();
            Set<String> aulasContabilizadas = aulasContabilizadasPorTurma
                    .computeIfAbsent(turmaId, ignorado -> new HashSet<>());
            if (aulaId == null || !aulasContabilizadas.add(aulaId)) {
                continue;
            }

            int duracao = Math.max(0, aula.getDuracaoPeriodos());
            duracaoPorTurma.merge(turmaId, duracao, Integer::sum);
            Set<PeriodoOcupado> periodos = periodosPorTurma
                    .computeIfAbsent(turmaId, ignorado -> new HashSet<>());

            HorarioPlanejamento horario = aula.getHorarioInicial();
            if (horario == null || duracao == 0) {
                continue;
            }

            for (int periodo = horario.getPeriodoInicial();
                    periodo < horario.getPeriodoInicial() + duracao;
                    periodo++) {
                periodos.add(new PeriodoOcupado(
                        horario.getDiaSemana(),
                        horario.getTurno(),
                        periodo
                ));
            }
        }

        if (periodosPorTurma.size() < 2) {
            return 0;
        }

        int totalPeriodos = duracaoPorTurma.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        Set<PeriodoOcupado> coincidencias = null;
        for (Set<PeriodoOcupado> periodos : periodosPorTurma.values()) {
            if (coincidencias == null) {
                coincidencias = new HashSet<>(periodos);
            } else {
                coincidencias.retainAll(periodos);
            }
        }

        int periodosSincronizados = coincidencias == null ? 0 : coincidencias.size();
        return Math.max(0, totalPeriodos - periodosSincronizados);
    }

    private record AulaVinculada(Integer turmaId, AulaPlanejamento aula) {

    }

    private record PeriodoOcupado(int diaSemana, int turno, int periodo) {

    }
}
