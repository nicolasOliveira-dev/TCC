package com.example.EduGrid.solver.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.solver.constraint.GradeConstraintProvider;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolutionUpdatePolicy;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;

/** Recalcula a pontuação e explica violações hard e soft sem alterar a grade recebida. */
public final class DiagnosticoOtimizacao {
    private static final int MAX_AULAS_INSPECIONADAS_POR_REGRA = 50;
    private static final Set<String> REGRAS_COM_PENALIDADE_PONDERADA = Set.of(
            "Sincronizacao de disciplinas tecnicas por grupo",
            "Sincronizacao de nivelamento por disciplina ano e turno",
            "Sincronizacao de eletivas por disciplina e turno");

    private DiagnosticoOtimizacao() {
    }

    public static final List<String> REGRAS = List.of(
            "Conflito de professor", "Conflito de turma", "Bloco ultrapassa limite do turno",
            "Turno incompativel com a turma", "Aulas nos dois ultimos periodos da quarta-feira",
            "Professor nao habilitado para a disciplina", "Professor indisponivel no horario",
            "Professor sem dia de planejamento", "Aula no dia de planejamento preferido",
            "Professor excedeu jornada maxima", "Descanso entre turnos",
            "Disciplinas consecutivas do mesmo professor",
            "Mesma disciplina tecnica simultanea no mesmo ano",
            "Sincronizacao de disciplinas tecnicas por grupo",
            "Sincronizacao de nivelamento por disciplina ano e turno",
            "Sincronizacao de eletivas por disciplina e turno");

    public static DiagnosticoGrade analisar(GradePlanejamento grade) {
        if (grade == null) {
            throw new IllegalArgumentException("A grade a analisar é obrigatória.");
        }

        Thread thread = Thread.currentThread();
        ClassLoader classLoaderAnterior = thread.getContextClassLoader();
        try {
            // O diagnóstico pode rodar no ForkJoinPool comum, fora do classloader
            // usado pelo DevTools para carregar as classes do domínio.
            thread.setContextClassLoader(grade.getClass().getClassLoader());
            return analisarComClassLoaderAtual(grade);
        } finally {
            thread.setContextClassLoader(classLoaderAnterior);
        }
    }

    public static String descrever(GradePlanejamento grade) {
        return descrever(grade, analisar(grade));
    }

    public static String descrever(GradePlanejamento grade, DiagnosticoGrade diagnostico) {
        if (grade == null || diagnostico == null) {
            throw new IllegalArgumentException("A grade e o diagnostico sao obrigatorios.");
        }
        StringBuilder texto = new StringBuilder(diagnostico.viavel()
                ? "A busca terminou com uma grade válida. Pontuação: "
                : "A busca terminou sem uma grade válida. Pontuação: ")
                .append(diagnostico.pontuacao()).append(".\n");
        if (!diagnostico.viavel()) {
            texto.append("Isso não prova que os dados sejam inviáveis.\n");
        }
        if (diagnostico.violacoes().isEmpty()) {
            texto.append("Nenhuma violação de restrição foi encontrada no recálculo.\n");
        }
        for (DiagnosticoGrade.Violacao violacao : diagnostico.violacoes()) {
            texto.append(violacao.restricao()).append(": ")
                    .append(descreverQuantidade(violacao))
                    .append(violacao.impacto()).append(" pontos; ")
                    .append(violacao.blocosEnvolvidos()).append(" bloco(s) apontado(s) em amostra de até 5.\n");
            for (String exemplo : violacao.exemplos()) {
                texto.append("  Bloco envolvido: ").append(exemplo).append('\n');
            }
        }
        return texto.toString();
    }

    public static String descreverViolacoesHard(GradePlanejamento grade) {
        DiagnosticoGrade diagnostico = analisar(grade);
        List<DiagnosticoGrade.Violacao> violacoesHard = diagnostico.violacoes().stream()
                .filter(violacao -> violacao.severidade() == DiagnosticoGrade.Severidade.HARD)
                .toList();
        if (violacoesHard.isEmpty()) {
            return "";
        }

        StringBuilder texto = new StringBuilder("Pontuação hard/soft: ")
                .append(diagnostico.pontuacao()).append(".\n")
                .append("A grade foi salva com violações hard. Revise os itens abaixo antes de utilizá-la.\n");
        for (DiagnosticoGrade.Violacao violacao : violacoesHard) {
            texto.append(violacao.restricao()).append(": ")
                    .append(descreverQuantidade(violacao))
                    .append(violacao.impacto()).append(" pontos; ")
                    .append(violacao.blocosEnvolvidos()).append(" bloco(s) apontado(s) em amostra de até 5.\n");
            for (String exemplo : violacao.exemplos()) {
                texto.append("  Bloco envolvido: ").append(exemplo).append('\n');
            }
        }
        return texto.toString();
    }

    private static DiagnosticoGrade analisarComClassLoaderAtual(GradePlanejamento grade) {
        GradePlanejamento copia = copiar(grade);
        HardSoftScore pontuacao = criarSolutionManager(null)
                .update(copia, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);
        List<DiagnosticoGrade.Violacao> violacoes = new ArrayList<>();

        for (String regra : REGRAS) {
            SolutionManager<GradePlanejamento, HardSoftScore> manager = criarSolutionManager(regra);
            copia.setAulas(grade.getAulas());
            HardSoftScore impacto = manager.update(copia, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);
            if (impacto.isZero()) {
                continue;
            }

            Set<String> exemplos = identificarBlocosEnvolvidos(grade, copia, manager, impacto);
            Integer quantidadeViolacoes = REGRAS_COM_PENALIDADE_PONDERADA.contains(regra)
                    ? null
                    : Math.toIntExact(Math.abs((long) (impacto.hardScore() != 0
                            ? impacto.hardScore() : impacto.softScore())));
            DiagnosticoGrade.Severidade severidade = impacto.hardScore() < 0
                    ? DiagnosticoGrade.Severidade.HARD
                    : DiagnosticoGrade.Severidade.SOFT;
            violacoes.add(new DiagnosticoGrade.Violacao(
                    regra,
                    severidade,
                    impacto,
                    quantidadeViolacoes,
                    exemplos.size(),
                    exemplos.stream().limit(5).toList()
            ));
        }

        return new DiagnosticoGrade(pontuacao, pontuacao.isFeasible(), violacoes);
    }

    private static Set<String> identificarBlocosEnvolvidos(
            GradePlanejamento grade,
            GradePlanejamento copia,
            SolutionManager<GradePlanejamento, HardSoftScore> manager,
            HardSoftScore pontuacaoOriginal) {
        Set<String> exemplos = new LinkedHashSet<>();
        int inspecionadas = 0;
        for (AulaPlanejamento aula : grade.getAulas()) {
            if (inspecionadas++ >= MAX_AULAS_INSPECIONADAS_POR_REGRA) {
                break;
            }
            List<AulaPlanejamento> aulasRestantes = new ArrayList<>(grade.getAulas());
            aulasRestantes.remove(aula);
            copia.setAulas(aulasRestantes);
            HardSoftScore pontuacaoSemBloco = manager.update(
                    copia, SolutionUpdatePolicy.UPDATE_SCORE_ONLY);
            if (pontuacaoSemBloco.compareTo(pontuacaoOriginal) > 0) {
                exemplos.add(descreverAula(aula));
                if (exemplos.size() == 5) {
                    break;
                }
            }
        }
        copia.setAulas(grade.getAulas());
        return exemplos;
    }

    private static String descreverQuantidade(DiagnosticoGrade.Violacao violacao) {
        return violacao.quantidadeViolacoes() == null
                ? "quantidade de violações não derivável do peso; "
                : violacao.quantidadeViolacoes() + " violação(ões); ";
    }

    private static SolutionManager<GradePlanejamento, HardSoftScore> criarSolutionManager(String diagnosticoNome) {
        ScoreDirectorFactoryConfig scoreConfig = new ScoreDirectorFactoryConfig()
                .withConstraintProviderClass(GradeConstraintProvider.class);
        if (diagnosticoNome != null) {
            scoreConfig.withConstraintProviderCustomProperties(Map.of("diagnosticoNome", diagnosticoNome));
        }
        SolverConfig config = new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withScoreDirectorFactory(scoreConfig);
        return SolutionManager.create(SolverFactory.<GradePlanejamento>create(config));
    }

    private static GradePlanejamento copiar(GradePlanejamento grade) {
        GradePlanejamento copia = new GradePlanejamento();
        copia.setAulas(grade.getAulas());
        copia.setProfessores(grade.getProfessores());
        copia.setHorariosPossiveis(grade.getHorariosPossiveis());
        copia.setTurmas(grade.getTurmas());
        copia.setGruposSincronizacao(grade.getGruposSincronizacao());
        copia.setVinculosSincronizacao(grade.getVinculosSincronizacao());
        return copia;
    }

    private static String descreverAula(AulaPlanejamento aula) {
        Turma turma = aula.getTurma();
        Disciplina disciplina = aula.getDisciplina();
        Professor professor = aula.getProfessor();
        HorarioPlanejamento horario = aula.getHorarioInicial();
        String nomeTurma = turma == null ? "Turma não informada" : turma.getNome();
        String nomeDisciplina = disciplina == null ? "Disciplina não informada" : disciplina.getNome();
        String nomeProfessor = professor == null ? "não atribuído" : professor.getNome();
        return nomeTurma + " / " + nomeDisciplina + " [" + aula.getId()
                + ", professor " + nomeProfessor
                + (horario == null ? "" : ", dia " + (horario.getDiaSemana() + 1)
                + ", turno " + horario.getTurno() + ", período " + (horario.getPeriodoInicial() + 1)) + "]";
    }
}
