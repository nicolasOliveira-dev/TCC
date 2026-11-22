package com.example.EduGrid.solver.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.solver.domain.GradePlanejamento;

import ai.timefold.solver.core.api.solver.SolverJob;

@Service
public class AcompanhamentoOtimizacaoService {

    private final OtimizacaoService otimizacaoService;

    private final Map<UUID, CompletableFuture<Integer>> jobs
            = new ConcurrentHashMap<>();

    public AcompanhamentoOtimizacaoService(
            OtimizacaoService otimizacaoService) {
        this.otimizacaoService = otimizacaoService;
    }

    public UUID iniciar(
            List<Turma> turmas,
            TamanhoGrade tamanhoGrade,
            GradeHoraria gradeHoraria) {

        UUID id = UUID.randomUUID();
        CompletableFuture<Integer> futuro = CompletableFuture.supplyAsync(() -> {
            Thread thread = Thread.currentThread();
            ClassLoader classLoaderAnterior = thread.getContextClassLoader();
            try {
                // O ForkJoinPool comum pode usar o classloader da aplicacao base,
                // enquanto o Spring DevTools carregou as classes do dominio no
                // RestartClassLoader. O Timefold exige a mesma Class para a
                // entidade registrada e para a entidade descoberta na solucao.
                thread.setContextClassLoader(GradePlanejamento.class.getClassLoader());
                SolverJob<GradePlanejamento> job = otimizacaoService.iniciarOtimizacao(
                        turmas, tamanhoGrade, gradeHoraria);
                job.getFinalBestSolution();
                return gradeHoraria.getId();
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha na otimizacao {}", id, e);
                throw new RuntimeException(e);
            } finally {
                thread.setContextClassLoader(classLoaderAnterior);
            }
        });
        jobs.put(id, futuro);

        return id;
    }

    public String consultarStatus(UUID id) {

        CompletableFuture<Integer> futuro
                = jobs.get(id);

        if (futuro == null) {
            return "NAO_ENCONTRADO";
        }

        if (!futuro.isDone()) {
            return "PROCESSANDO";
        }

        if (futuro.isCompletedExceptionally()) {
            return "FALHOU";
        }

        return "CONCLUIDO";
    }

    public Integer consultarGradeId(UUID id) {
        CompletableFuture<Integer> futuro = jobs.get(id);
        if (futuro == null || !futuro.isDone() || futuro.isCompletedExceptionally()) {
            return null;
        }
        return futuro.getNow(null);
    }

    public String consultarErro(UUID id) {
        var futuro = jobs.get(id);
        if (futuro == null || !futuro.isCompletedExceptionally()) {
            return null;
        }
        try {
            futuro.join();
            return null;
        } catch (java.util.concurrent.CompletionException e) {
            Throwable causa = e;
            while (causa != null) {
                if (causa instanceof DiagnosticoOtimizacaoException) {
                    return causa.getMessage();
                }
                causa = causa.getCause();
            }
            return "Nao foi possivel concluir a otimizacao. Consulte o log da aplicacao para os detalhes.";
        }
    }
}
