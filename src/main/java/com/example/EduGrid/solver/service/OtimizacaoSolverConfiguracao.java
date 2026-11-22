package com.example.EduGrid.solver.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.EduGrid.solver.constraint.GradeConstraintProvider;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

@Configuration
public class OtimizacaoSolverConfiguracao {

    @Bean(destroyMethod = "close")
    public SolverManager<GradePlanejamento> solverManager(
            @Value("${edugrid.solver.spent-limit:30s}") Duration spentLimit) {
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(GradePlanejamento.class)
                .withEntityClasses(AulaPlanejamento.class)
                .withConstraintProviderClass(GradeConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig().withSpentLimit(spentLimit)
                );
        return SolverManager.create(
                SolverFactory.create(solverConfig),
                new SolverManagerConfig()
        );
    }
}
