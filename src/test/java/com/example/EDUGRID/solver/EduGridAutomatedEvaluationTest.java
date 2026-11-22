package com.example.EduGrid.solver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.CursoRepository;
import com.example.EduGrid.repository.DisciplinaRepository;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.ProfessorRepository;
import com.example.EduGrid.repository.TurmaRepository;
import com.example.EduGrid.service.BlocoDisciplinaService;
import com.example.EduGrid.service.ImportacaoService;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.service.AcompanhamentoOtimizacaoService;
import com.example.EduGrid.solver.service.DiagnosticoGrade;
import com.example.EduGrid.solver.service.DiagnosticoOtimizacao;
import com.example.EduGrid.solver.service.IdentificacaoSincronizacaoService;
import com.example.EduGrid.solver.service.NovaVersaoGradeService;
import com.example.EduGrid.solver.service.OtimizacaoService;
import com.example.EduGrid.solver.service.OtimizacaoSolverConfiguracao;
import com.example.EduGrid.solver.service.PersistenciaGradeService;

import ai.timefold.solver.core.api.solver.SolverManager;

/**
 * Opt-in end-to-end evaluation. It imports the project workbook into in-memory
 * repository doubles and runs the real EduGrid services and Timefold solver.
 * No application database is opened or modified.
 */
class EduGridAutomatedEvaluationTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int REPETICOES = 3;
    private static final Duration LIMITE_ESPERA_POR_ETAPA = Duration.ofMinutes(12);

    @Test
    @EnabledIfSystemProperty(named = "edugrid.evaluation", matches = "true")
    void executarAvaliacaoReproduzivelComPlanilhaDoProjeto() throws Exception {
        Path projeto = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path planilha = projeto.resolve(System.getProperty(
                "edugrid.evaluation.dataset",
                "outputs/edugrid-importacao-adaptada/EduGrid_Base_Teste_Importacao_Adaptada.xlsx"));
        Path relatorio = projeto.resolve(System.getProperty(
                "edugrid.evaluation.report",
                "outputs/evaluation/avaliacao-edugrid.md"));
        int limiteSegundos = Integer.parseInt(System.getProperty("edugrid.evaluation.limit-seconds", "120"));
        Duration limiteSolver = Duration.ofSeconds(limiteSegundos);

        assertTrue(Files.isRegularFile(planilha), "Planilha de avaliação não encontrada: " + planilha);
        OffsetDateTime inicioAvaliacao = OffsetDateTime.now(ZONE);
        List<String> linhasRelatorio = new ArrayList<>();
        List<ExecucaoNovaVersao> execucoes = new ArrayList<>();
        String falha = null;
        Dataset dataset = null;
        MedidaGeracao geracaoBase = null;

        try {
            RepositoriosMemoria memoria = new RepositoriosMemoria();
            dataset = importarDados(planilha, memoria);
            validarDimensoesDisponibilidade(dataset);

            TamanhoGrade tamanho = new TamanhoGrade(
                    dataset.turnos(), dataset.periodosPorDia(), dataset.diasPorSemana());
            tamanho.setId(1);
            dataset.turmas().forEach(turma -> {
                turma.setTamanhoGrade(tamanho);
                turma.getDisciplinas().forEach(disciplina -> disciplina.setTamanhoGrade(tamanho));
            });

            PersistenciaGradeService persistencia = memoria.criarPersistenciaReal();
            AtomicReference<GradePlanejamento> solucaoInicial = new AtomicReference<>();
            memoria.capturarSolucaoInicial = solucaoInicial;

            SolverManager<GradePlanejamento> solverManager = new OtimizacaoSolverConfiguracao()
                    .solverManager(limiteSolver);
            try {
                OtimizacaoService otimizacao = new OtimizacaoService(
                        new BlocoDisciplinaService(),
                        new IdentificacaoSincronizacaoService(),
                        memoria.horarioRepository,
                        memoria.professorRepositoryComDados(dataset.professores()),
                        solverManager,
                        persistencia);

                GradeHoraria gradeBase = new GradeHoraria(new ArrayList<>(dataset.turmas()), tamanho);
                OffsetDateTime inicio = OffsetDateTime.now(ZONE);
                long relogioInicio = System.nanoTime();
                AcompanhamentoOtimizacaoService acompanhamento = new AcompanhamentoOtimizacaoService(otimizacao);
                var jobId = acompanhamento.iniciar(dataset.turmas(), tamanho, gradeBase);
                aguardarStatus(() -> acompanhamento.consultarStatus(jobId), "CONCLUIDO", "FALHOU");
                String status = acompanhamento.consultarStatus(jobId);
                if (!"CONCLUIDO".equals(status)) {
                    throw new IllegalStateException("Geração inicial falhou: " + acompanhamento.consultarErro(jobId));
                }
                long duracaoPipeline = System.nanoTime() - relogioInicio;
                OffsetDateTime fim = OffsetDateTime.now(ZONE);
                GradePlanejamento solucao = solucaoInicial.get();
                assertNotNull(solucao, "A persistência não recebeu a solução inicial do solver.");
                long relogioDiagnostico = System.nanoTime();
                DiagnosticoGrade diagnostico = DiagnosticoOtimizacao.analisar(solucao);
                long duracaoDiagnostico = System.nanoTime() - relogioDiagnostico;
                geracaoBase = new MedidaGeracao(inicio, fim, duracaoPipeline, duracaoDiagnostico,
                        solucao.getScore().toString(), diagnostico);

                NovaVersaoGradeService novasVersoes = new NovaVersaoGradeService(
                        memoria.gradeRepository,
                        memoria.horarioRepository,
                        otimizacao,
                        persistencia);
                for (int repeticao = 1; repeticao <= REPETICOES; repeticao++) {
                    OffsetDateTime inicioVersao = OffsetDateTime.now(ZONE);
                    long inicioVersaoNanos = System.nanoTime();
                    var tentativaId = novasVersoes.iniciar(gradeBase.getId(), false);
                    aguardarStatus(() -> novasVersoes.status(tentativaId), "CONCLUIDO", "FALHOU");
                    var resumo = novasVersoes.resumo(tentativaId);
                    long duracaoVersao = System.nanoTime() - inicioVersaoNanos;
                    OffsetDateTime fimVersao = OffsetDateTime.now(ZONE);
                    boolean aceita = Boolean.TRUE.equals(resumo.podeAplicar());
                    Integer novaGradeId = null;
                    String erroAplicacao = "";
                    if (aceita) {
                        try {
                            novaGradeId = novasVersoes.aplicar(tentativaId);
                        } catch (RuntimeException e) {
                            aceita = false;
                            erroAplicacao = e.getMessage();
                        }
                    }
                    execucoes.add(new ExecucaoNovaVersao(repeticao, inicioVersao, fimVersao,
                            duracaoVersao, resumo.semente() == null ? "não informada" : resumo.semente().toString(),
                            resumo.pontuacaoAtual(), resumo.pontuacaoCandidata(), resumo.viavel(),
                            Boolean.TRUE.equals(resumo.melhorou()), aceita, novaGradeId,
                            resumo.erro(), erroAplicacao, resumo.diagnostico()));
                }
            } finally {
                solverManager.close();
            }
        } catch (Exception e) {
            falha = causaRaiz(e).getClass().getSimpleName() + ": " + causaRaiz(e).getMessage();
        }

        OffsetDateTime fimAvaliacao = OffsetDateTime.now(ZONE);
        criarRelatorio(relatorio, planilha, limiteSolver, inicioAvaliacao, fimAvaliacao,
                dataset, geracaoBase, execucoes, falha);
        System.out.println("Relatório de avaliação: " + relatorio);
        System.out.println(Files.readString(relatorio));

        assertTrue(falha == null, "A avaliação falhou: " + falha + ". Veja " + relatorio);
        assertNotNull(geracaoBase, "Não foi possível medir a geração inicial. Veja " + relatorio);
        assertEquals(REPETICOES, execucoes.size(), "Nem todas as três tentativas foram medidas.");
    }

    @Test
    @EnabledIfSystemProperty(named = "edugrid.evaluation.roundtrip", matches = "true")
    void diagnosticarDiferencaEntreSolucaoEmMemoriaEGradeReconstruida() throws Exception {
        Path projeto = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path planilha = projeto.resolve(System.getProperty("edugrid.evaluation.dataset",
                "outputs/edugrid-importacao-adaptada/EduGrid_Base_Teste_Importacao_Adaptada.xlsx"));
        Path destino = projeto.resolve("outputs/evaluation/diagnostico-reconstrucao-grade.md");
        int limiteSegundos = Integer.parseInt(System.getProperty("edugrid.evaluation.limit-seconds", "120"));
        RepositoriosMemoria memoria = new RepositoriosMemoria();
        Dataset dataset = importarDados(planilha, memoria);
        validarDimensoesDisponibilidade(dataset);
        TamanhoGrade tamanho = new TamanhoGrade(dataset.turnos(), dataset.periodosPorDia(), dataset.diasPorSemana());
        tamanho.setId(1);
        dataset.turmas().forEach(turma -> {
            turma.setTamanhoGrade(tamanho);
            turma.getDisciplinas().forEach(disciplina -> disciplina.setTamanhoGrade(tamanho));
        });
        PersistenciaGradeService persistencia = memoria.criarPersistenciaReal();
        AtomicReference<GradePlanejamento> solucaoInicial = new AtomicReference<>();
        memoria.capturarSolucaoInicial = solucaoInicial;
        SolverManager<GradePlanejamento> solverManager = new OtimizacaoSolverConfiguracao()
                .solverManager(Duration.ofSeconds(limiteSegundos));
        try {
            OtimizacaoService otimizacao = new OtimizacaoService(new BlocoDisciplinaService(),
                    new IdentificacaoSincronizacaoService(), memoria.horarioRepository,
                    memoria.professorRepositoryComDados(dataset.professores()), solverManager, persistencia);
            GradeHoraria gradeBase = new GradeHoraria(new ArrayList<>(dataset.turmas()), tamanho);
            AcompanhamentoOtimizacaoService acompanhamento = new AcompanhamentoOtimizacaoService(otimizacao);
            var jobId = acompanhamento.iniciar(dataset.turmas(), tamanho, gradeBase);
            aguardarStatus(() -> acompanhamento.consultarStatus(jobId), "CONCLUIDO", "FALHOU");
            if (!"CONCLUIDO".equals(acompanhamento.consultarStatus(jobId))) {
                throw new IllegalStateException("Geração inicial falhou: " + acompanhamento.consultarErro(jobId));
            }
            GradePlanejamento original = solucaoInicial.get();
            assertNotNull(original);
            GradePlanejamento reconstruida = otimizacao.montarProblema(
                    gradeBase.getTurmas(), gradeBase.getTamanhoGrade(), gradeBase, false);
            DiagnosticoGrade diagnosticoOriginal = DiagnosticoOtimizacao.analisar(original);
            DiagnosticoGrade diagnosticoReconstruido = DiagnosticoOtimizacao.analisar(reconstruida);
            Map<String, AulaPlanejamento> originais = original.getAulas().stream()
                    .collect(java.util.stream.Collectors.toMap(AulaPlanejamento::getId, aula -> aula));
            int posicoesDiferentes = 0, professoresDiferentes = 0, posicoesAusentes = 0, professoresAusentes = 0;
            for (AulaPlanejamento aula : reconstruida.getAulas()) {
                AulaPlanejamento anterior = originais.get(aula.getId());
                if (aula.getHorarioInicial() == null) posicoesAusentes++;
                if (aula.getProfessor() == null) professoresAusentes++;
                if (anterior == null) continue;
                if (!mesmaPosicao(anterior.getHorarioInicial(), aula.getHorarioInicial())) posicoesDiferentes++;
                Integer professorAnterior = anterior.getProfessor() == null ? null : anterior.getProfessor().getId();
                Integer professorReconstruido = aula.getProfessor() == null ? null : aula.getProfessor().getId();
                if (!java.util.Objects.equals(professorAnterior, professorReconstruido)) professoresDiferentes++;
            }
            StringBuilder md = new StringBuilder("# Diagnóstico da diferença de pontuação\n\n")
                    .append("Arquivo de dados: ").append(planilha.getFileName()).append("; repositórios em memória.\n")
                    .append("Aulas: solução inicial ").append(original.getAulas().size())
                    .append("; reconstruída ").append(reconstruida.getAulas().size()).append(".\n")
                    .append("Pontuação inicial: ").append(diagnosticoOriginal.pontuacao())
                    .append("; pontuação reconstruída: ").append(diagnosticoReconstruido.pontuacao()).append(".\n")
                    .append("Grupos de sincronização: inicial ").append(original.getGruposSincronizacao().size())
                    .append("; reconstruído ").append(reconstruida.getGruposSincronizacao().size()).append(".\n")
                    .append("Vínculos de sincronização: inicial ").append(original.getVinculosSincronizacao().size())
                    .append("; reconstruído ").append(reconstruida.getVinculosSincronizacao().size()).append(".\n")
                    .append("Posições diferentes: ").append(posicoesDiferentes)
                    .append("; posições ausentes: ").append(posicoesAusentes).append(".\n")
                    .append("Professores diferentes: ").append(professoresDiferentes)
                    .append("; professores ausentes: ").append(professoresAusentes).append(".\n\n")
                    .append("Restrições da solução inicial:\n\n");
            appendViolations(md, diagnosticoOriginal);
            md.append("Restrições da grade reconstruída:\n\n");
            appendViolations(md, diagnosticoReconstruido);
            Files.createDirectories(destino.getParent());
            Files.writeString(destino, md.toString());
            System.out.println("Diagnóstico round-trip: " + destino + "\n" + md);
        } finally {
            solverManager.close();
        }
    }

    private boolean mesmaPosicao(com.example.EduGrid.solver.domain.HorarioPlanejamento a,
            com.example.EduGrid.solver.domain.HorarioPlanejamento b) {
        if (a == null || b == null) return a == b;
        return a.getDiaSemana() == b.getDiaSemana() && a.getTurno() == b.getTurno()
                && a.getPeriodoInicial() == b.getPeriodoInicial();
    }

    private Dataset importarDados(Path planilha, RepositoriosMemoria memoria) throws IOException {
        var arquivo = new MockMultipartFile("arquivo", planilha.getFileName().toString(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(planilha));
        ImportacaoService importador = new ImportacaoService(memoria.cursoRepository,
                memoria.disciplinaRepository, memoria.turmaRepository, memoria.professorRepository);
        ImportacaoService.ResultadoImportacao resultado = importador.importar(arquivo);
        return new Dataset(memoria.cursos, memoria.disciplinas, memoria.turmas,
                memoria.professores, resultado);
    }

    private void validarDimensoesDisponibilidade(Dataset dataset) {
        assertTrue(!dataset.turmas().isEmpty(), "A planilha não contém turmas.");
        assertTrue(!dataset.professores().isEmpty(), "A planilha não contém professores.");
        Professor professor = dataset.professores().get(0);
        Boolean[][][] disponibilidade = professor.getDisponibilidadeHoraria();
        assertNotNull(disponibilidade, "O conjunto de dados precisa incluir disponibilidades docentes.");
        assertTrue(disponibilidade.length >= 3, "São esperados os três turnos do EduGrid.");
        assertTrue(disponibilidade[0].length > 0 && disponibilidade[0][0].length > 0,
                "A matriz de disponibilidade está vazia.");
        int periodos = disponibilidade[0].length;
        int dias = disponibilidade[0][0].length;
        for (Professor p : dataset.professores()) {
            Boolean[][][] matriz = p.getDisponibilidadeHoraria();
            assertNotNull(matriz, "Disponibilidade ausente para " + p.getNome());
            assertTrue(matriz.length >= 3, "Matriz sem três turnos para " + p.getNome());
            for (int turno = 0; turno < 3; turno++) {
                assertTrue(matriz[turno].length >= periodos,
                        "Matriz de disponibilidade menor que a configuração para " + p.getNome());
                for (int periodo = 0; periodo < periodos; periodo++) {
                    assertTrue(matriz[turno][periodo].length >= dias,
                            "Matriz de dias incompleta para " + p.getNome());
                }
            }
        }
    }

    private void aguardarStatus(java.util.function.Supplier<String> status,
            String concluido, String falhou) throws InterruptedException {
        long limite = System.nanoTime() + LIMITE_ESPERA_POR_ETAPA.toNanos();
        while (System.nanoTime() < limite) {
            String atual = status.get();
            if (concluido.equals(atual) || falhou.equals(atual)) return;
            Thread.sleep(250L);
        }
        throw new IllegalStateException("A etapa excedeu o limite de espera de "
                + LIMITE_ESPERA_POR_ETAPA.toMinutes() + " minutos.");
    }

    private void criarRelatorio(Path destino, Path planilha, Duration limiteSolver,
            OffsetDateTime inicio, OffsetDateTime fim, Dataset dataset,
            MedidaGeracao geracao, List<ExecucaoNovaVersao> execucoes, String falha) throws IOException {
        Files.createDirectories(destino.getParent());
        StringBuilder md = new StringBuilder();
        md.append("# Avaliação automatizada do EduGrid\n\n")
                .append("- Início da avaliação: ").append(TIME_FORMAT.format(inicio)).append("\n")
                .append("- Fim da avaliação: ").append(TIME_FORMAT.format(fim)).append("\n")
                .append("- Java: ").append(System.getProperty("java.version")).append("\n")
                .append("- Maven Wrapper: 3.9.16; comando: `.\\mvnw.cmd '-Dtest=EduGridAutomatedEvaluationTest' '-Dedugrid.evaluation=true' '-Dedugrid.evaluation.limit-seconds=120' test`.\n")
                .append("- Sistema operacional: ").append(System.getProperty("os.name"))
                .append(" ").append(System.getProperty("os.version")).append("\n")
                .append("- Timefold Solver: 2.7.0; limite por busca: ").append(limiteSolver).append("\n")
                .append("- Base de dados: planilha realista do projeto `")
                .append(Path.of(System.getProperty("user.dir")).toAbsolutePath().relativize(planilha.toAbsolutePath()))
                .append("`; nenhum banco de desenvolvimento/produção foi acessado.\n")
                .append("- Isolamento: `ImportacaoService` real com repositórios em memória; serviços de geração, otimização, diagnóstico e persistência do EduGrid executados; solver Timefold real.\n")
                .append("- Erro `AulaPlanejamento` não reconhecida: a configuração declara a entidade na solução e no solver; a execução de avaliação não reproduziu o erro. O teste roda sem o classloader de reinicialização do DevTools, portanto a causa ligada ao classloader precisa de validação final numa execução normal da aplicação.\n")
                .append("- Ajustes manuais: fora do escopo e não medidos.\n\n");

        if (dataset != null) {
            md.append("## Dados importados\n\n")
                    .append("Cursos: ").append(dataset.importacao().cursos())
                    .append("; disciplinas: ").append(dataset.importacao().disciplinas())
                    .append("; turmas: ").append(dataset.importacao().turmas())
                    .append("; professores: ").append(dataset.importacao().professores()).append(".\n")
                    .append("Configuração da grade: ").append(dataset.turnos())
                    .append(" turnos, ").append(dataset.periodosPorDia()).append(" períodos/dia, ")
                    .append(dataset.diasPorSemana()).append(" dias/semana, período de 45 minutos.\n\n");
        }

        md.append("## Critérios e interpretação\n\n")
                .append("A geração inicial é medida desde a chamada ao serviço assíncrono até a conclusão da busca, diagnóstico hard e persistência em repositórios em memória. O diagnóstico completo de hard e soft é cronometrado à parte. Cada tentativa de nova versão é medida desde `NovaVersaoGradeService.iniciar` até o resultado terminal, incluindo preparação, busca e diagnóstico. A semente das novas versões é a fornecida pelo próprio serviço.\n\n")
                .append("Viabilidade segue `HardSoftScore.isFeasible()`. A aceitação segue o código atual: solução viável e pontuação estritamente melhor que a grade de origem; quando aceita, `aplicar` é executado no repositório em memória. Nas restrições com penalidade unitária, a quantidade de violações corresponde ao impacto no score; as três sincronizações usam pesos e sua quantidade não pode ser deduzida apenas pela pontuação. A busca de exemplos inspeciona no máximo 50 aulas por restrição e registra até cinco aulas identificadas, portanto esse campo é uma amostra, não o total de blocos conflitantes.\n\n");

        if (geracao != null) {
            md.append("## Geração da grade de referência\n\n")
                    .append("- Início/fim medidos: ").append(TIME_FORMAT.format(geracao.inicio()))
                    .append(" — ").append(TIME_FORMAT.format(geracao.fim())).append("\n")
                    .append("- Tempo do pipeline: ").append(formatarDuracao(geracao.pipelineNanos())).append("\n")
                    .append("- Tempo do diagnóstico detalhado: ").append(formatarDuracao(geracao.diagnosticoNanos())).append("\n")
                    .append("- Pontuação: `").append(geracao.pontuacao()).append("`; viável: ")
                    .append(geracao.diagnostico().viavel()).append("; semente: não exposta pelo fluxo de geração inicial.\n\n")
                    .append("Violações identificadas:\n\n");
            appendViolations(md, geracao.diagnostico());
        }

        md.append("## Três tentativas de nova versão\n\n")
                .append("| Execução | Início → fim | Tempo | Semente | Pontuação inicial → candidata | Viável | Aceita/aplicada | Restrições apontadas |\n")
                .append("|---:|---|---:|---:|---|:---:|:---:|---|\n");
        for (ExecucaoNovaVersao e : execucoes) {
            String score = valor(e.pontuacaoAtual()) + " → " + valor(e.pontuacaoCandidata());
            String restricoes = resumirViolacoes(e.diagnostico());
            String aceita = e.erro() != null ? "falhou" : e.aceita() ? "sim (grade " + e.novaGradeId() + ")" : "não";
            md.append("| ").append(e.numero()).append(" | ")
                    .append(TIME_FORMAT.format(e.inicio())).append(" → ").append(TIME_FORMAT.format(e.fim()))
                    .append(" | ").append(formatarDuracao(e.duracaoNanos()))
                    .append(" | ").append(e.semente()).append(" | ").append(score)
                    .append(" | ").append(e.viavel()).append(" | ").append(aceita)
                    .append(" | ").append(restricoes).append(" |\n");
        }
        if (execucoes.size() == REPETICOES) {
            double media = execucoes.stream().mapToLong(ExecucaoNovaVersao::duracaoNanos).average().orElse(0);
            md.append("\nTempo médio das três tentativas: **").append(formatarDuracao((long) media)).append("**.\n\n");
        }
        for (ExecucaoNovaVersao e : execucoes) {
            md.append("### Execução ").append(e.numero()).append(" — violações\n\n");
            if (e.erro() != null) {
                md.append("Falha: ").append(e.erro()).append("\n\n");
            } else {
                md.append(e.diagnostico() == null || e.diagnostico().isBlank()
                        ? "Nenhuma violação informada pelo diagnóstico.\n\n"
                        : "```text\n" + e.diagnostico().strip() + "\n```\n\n");
            }
            if (e.erroAplicacao() != null && !e.erroAplicacao().isBlank()) {
                md.append("Falha ao aplicar a candidata: ").append(e.erroAplicacao()).append("\n\n");
            }
        }

        if (falha != null) {
            md.append("## Falha da avaliação\n\n").append(falha).append("\n\n")
                    .append("Etapas ainda não concluídas não possuem resultados medidos.\n");
        }
        md.append("\n## Texto-base para o artigo\n\n");
        if (geracao == null || execucoes.size() < REPETICOES || falha != null) {
            md.append("A avaliação não foi concluída integralmente; não há resultados suficientes para redigir uma conclusão quantitativa. Consulte a falha registrada acima.\n");
        } else {
            long aceitas = execucoes.stream().filter(ExecucaoNovaVersao::aceita).count();
            long viaveis = execucoes.stream().filter(ExecucaoNovaVersao::viavel).count();
            double media = execucoes.stream().mapToLong(ExecucaoNovaVersao::duracaoNanos).average().orElse(0);
            md.append("Com a planilha realista do projeto (").append(dataset.importacao().turmas())
                    .append(" turmas, ").append(dataset.importacao().disciplinas())
                    .append(" disciplinas e ").append(dataset.importacao().professores())
                    .append(" docentes), a geração de referência terminou com pontuação `")
                    .append(geracao.pontuacao()).append("` e foi ")
                    .append(geracao.diagnostico().viavel() ? "viável" : "inviável")
                    .append(". Nas três tentativas de nova versão, ").append(viaveis)
                    .append(" foram viáveis e ").append(aceitas)
                     .append(" atenderam ao critério atual de aplicação. O tempo médio medido das tentativas foi ")
                    .append(formatarDuracao((long) media))
                    .append(". O fluxo de nova versão recalculou a grade de referência com pontuação ")
                    .append(execucoes.get(0).pontuacaoAtual())
                    .append(", diferente da pontuação final da geração inicial; essa divergência precisa ser investigada antes de interpretar os valores como melhoria quantitativa. Ajustes manuais não foram avaliados.\n");
        }
        Files.writeString(destino, md.toString());
    }

    private void appendViolations(StringBuilder md, DiagnosticoGrade diagnostico) {
        if (diagnostico.violacoes().isEmpty()) {
            md.append("- Nenhuma violação identificada.\n\n");
            return;
        }
        for (DiagnosticoGrade.Violacao v : diagnostico.violacoes()) {
            md.append("- ").append(v.severidade()).append(" — ").append(v.restricao())
                    .append(": ").append(v.quantidadeViolacoes() == null
                            ? "quantidade não derivável do peso"
                            : v.quantidadeViolacoes() + " match(es) (contagem exata; peso unitário)")
                    .append("; impacto ").append(v.impacto()).append("; blocos distintos encontrados na amostra: ")
                    .append(v.blocosEnvolvidos()).append(" (até 50 aulas inspecionadas; até 5 exemplos exibidos).\n");
        }
        md.append('\n');
    }

    private String resumirViolacoes(String diagnostico) {
        if (diagnostico == null || diagnostico.isBlank()) return "não informado";
        List<String> linhas = diagnostico.lines()
                .filter(l -> l.contains(" pontos;") && l.contains("amostra de até 5"))
                .map(String::strip).toList();
        return linhas.isEmpty() ? "nenhuma informada" : String.join("; ", linhas);
    }

    private String formatarDuracao(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.3f s", nanos / 1_000_000_000.0);
    }

    private String valor(String valor) { return valor == null ? "não disponível" : valor; }

    private Throwable causaRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual.getCause() != null && atual.getCause() != atual) atual = atual.getCause();
        return atual;
    }

    private record Dataset(List<Curso> cursos, List<Disciplina> disciplinas, List<Turma> turmas,
            List<Professor> professores, ImportacaoService.ResultadoImportacao importacao) {
        int turnos() { return professores.get(0).getDisponibilidadeHoraria().length; }
        int periodosPorDia() { return professores.get(0).getDisponibilidadeHoraria()[0].length; }
        int diasPorSemana() { return professores.get(0).getDisponibilidadeHoraria()[0][0].length; }
    }

    private record MedidaGeracao(OffsetDateTime inicio, OffsetDateTime fim,
            long pipelineNanos, long diagnosticoNanos, String pontuacao, DiagnosticoGrade diagnostico) {}

    private record ExecucaoNovaVersao(int numero, OffsetDateTime inicio, OffsetDateTime fim,
            long duracaoNanos, String semente, String pontuacaoAtual, String pontuacaoCandidata,
            boolean viavel, boolean melhorou, boolean aceita, Integer novaGradeId,
            String erro, String erroAplicacao, String diagnostico) {}

    private static final class RepositoriosMemoria {
        private final CursoRepository cursoRepository = mock(CursoRepository.class);
        private final DisciplinaRepository disciplinaRepository = mock(DisciplinaRepository.class);
        private final TurmaRepository turmaRepository = mock(TurmaRepository.class);
        private final ProfessorRepository professorRepository = mock(ProfessorRepository.class);
        private final HorarioRepository horarioRepository = mock(HorarioRepository.class);
        private final GradeHorariaRepository gradeRepository = mock(GradeHorariaRepository.class);
        private final List<Curso> cursos = new CopyOnWriteArrayList<>();
        private final List<Disciplina> disciplinas = new CopyOnWriteArrayList<>();
        private final List<Turma> turmas = new CopyOnWriteArrayList<>();
        private final List<Professor> professores = new CopyOnWriteArrayList<>();
        private final List<Horario> horarios = new CopyOnWriteArrayList<>();
        private final Map<Integer, GradeHoraria> grades = new ConcurrentHashMap<>();
        private final AtomicInteger idCurso = new AtomicInteger(100);
        private final AtomicInteger idDisciplina = new AtomicInteger(1000);
        private final AtomicInteger idTurma = new AtomicInteger(2000);
        private final AtomicInteger idHorario = new AtomicInteger(1);
        private final AtomicInteger idGrade = new AtomicInteger(1);
        private AtomicReference<GradePlanejamento> capturarSolucaoInicial;

        RepositoriosMemoria() {
            when(cursoRepository.findAll()).thenReturn(List.of());
            when(disciplinaRepository.findAll()).thenReturn(List.of());
            when(turmaRepository.findAll()).thenReturn(List.of());
            when(professorRepository.findAll()).thenReturn(List.of());
            doAnswer(inv -> {
                Curso item = inv.getArgument(0); item.setId(idCurso.incrementAndGet()); cursos.add(item); return item;
            }).when(cursoRepository).save(any(Curso.class));
            doAnswer(inv -> {
                Disciplina item = inv.getArgument(0); item.setId(idDisciplina.incrementAndGet()); disciplinas.add(item); return item;
            }).when(disciplinaRepository).save(any(Disciplina.class));
            doAnswer(inv -> {
                Turma item = inv.getArgument(0); item.setId(idTurma.incrementAndGet()); turmas.add(item); return item;
            }).when(turmaRepository).save(any(Turma.class));
            doAnswer(inv -> {
                Professor item = inv.getArgument(0); item.setId(professores.size() + 1); professores.add(item); return item;
            }).when(professorRepository).save(any(Professor.class));

            when(gradeRepository.findById(any())).thenAnswer(inv ->
                    Optional.ofNullable(grades.get(inv.getArgument(0))));
            doAnswer(inv -> {
                GradeHoraria item = inv.getArgument(0);
                if (item.getId() == null) item.setId(idGrade.incrementAndGet());
                grades.put(item.getId(), item);
                return item;
            }).when(gradeRepository).save(any(GradeHoraria.class));
            when(horarioRepository.findByGradeHoraria(any())).thenAnswer(inv -> {
                GradeHoraria grade = inv.getArgument(0);
                return horarios.stream().filter(h -> h.getGradeHoraria() != null
                        && java.util.Objects.equals(h.getGradeHoraria().getId(), grade.getId())).toList();
            });
            when(horarioRepository.findByTurmaInAndGradeHorariaIsNull(anyCollection())).thenAnswer(inv -> {
                @SuppressWarnings("unchecked") List<Turma> alvo = new ArrayList<>((java.util.Collection<Turma>) inv.getArgument(0));
                return horarios.stream().filter(h -> h.getGradeHoraria() == null && alvo.contains(h.getTurma())).toList();
            });
            when(horarioRepository.saveAll(anyIterable())).thenAnswer(inv -> {
                List<Horario> salvos = new ArrayList<>();
                for (Object value : (Iterable<?>) inv.getArgument(0)) {
                    Horario horario = (Horario) value;
                    if (horario.getId() == null) horario.setId(idHorario.incrementAndGet());
                    horarios.add(horario);
                    salvos.add(horario);
                }
                return salvos;
            });
            doAnswer(inv -> null).when(horarioRepository).deleteByTurma(any(Turma.class));
        }

        ProfessorRepository professorRepositoryComDados(List<Professor> dados) {
            when(professorRepository.findAllComDisciplinasPossiveis()).thenReturn(dados);
            return professorRepository;
        }

        PersistenciaGradeService criarPersistenciaReal() {
            return new PersistenciaGradeService(horarioRepository, gradeRepository) {
                @Override
                public GradeHoraria persistirResultado(GradePlanejamento solucao,
                        GradeHoraria grade, String relatorioHard) {
                    if (capturarSolucaoInicial != null && capturarSolucaoInicial.get() == null) {
                        capturarSolucaoInicial.set(solucao);
                    }
                    return super.persistirResultado(solucao, grade, relatorioHard);
                }
            };
        }
    }
}
