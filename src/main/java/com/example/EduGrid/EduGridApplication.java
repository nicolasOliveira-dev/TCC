package com.example.EduGrid;

import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.EduGrid.service.BlocoDisciplinaService;
import com.example.EduGrid.service.DisciplinaService;
import com.example.EduGrid.service.ProfessorService;
import com.example.EduGrid.service.TamanhoGradeService;
import com.example.EduGrid.service.TurmaService;
import com.example.EduGrid.solver.service.DiagnosticoOtimizacaoException;
import com.example.EduGrid.solver.service.OtimizacaoService;
import com.example.EduGrid.solver.service.PreparacaoOtimizacao;

@SpringBootApplication
public class EduGridApplication {
    private static final Logger log = LoggerFactory.getLogger(EduGridApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EduGridApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "edugrid.dados.adequar-na-inicializacao",
            havingValue = "true", matchIfMissing = true)
    CommandLineRunner adequarDadosNaInicializacao(
            ProfessorService professores,
            DisciplinaService disciplinas,
            TurmaService turmas,
            TamanhoGradeService configuracoes,
            BlocoDisciplinaService blocos,
            OtimizacaoService otimizacao,
            PlatformTransactionManager transactionManager) {
        return args -> {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                        adequarDados(professores, disciplinas, turmas, configuracoes, blocos, otimizacao));
            } catch (RuntimeException e) {
                // A transacao e revertida; falha nos dados nao impede subir a aplicacao.
                log.error("Adequacao dos dados nao concluida. Confira a configuracao e os cadastros.", e);
            }
        };
    }

    static void adequarDados(
            ProfessorService professores,
            DisciplinaService disciplinas,
            TurmaService turmas,
            TamanhoGradeService configuracoes,
            BlocoDisciplinaService blocos,
            OtimizacaoService otimizacao) {
        var configuracao = configuracoes.buscarConfiguracao();
        if (configuracao == null || configuracao.getQtdeDiasSemana() < 1
                || configuracao.getQtdeDiasSemana() > 7 || configuracao.getQtdePeriodosDia() < 1
                || configuracao.getQtdePeriodosDia() > 12 || configuracao.getQtdeTurnos() < 1
                || configuracao.getQtdeTurnos() > 3 || configuracao.getTamanhoPeriodo() <= 0) {
            throw new IllegalStateException("Cadastre uma configuracao de grade valida antes de adequar os dados.");
        }

        int disciplinasAlteradas = 0;
        for (var disciplina : disciplinas.listarDisciplinas()) {
            if (!Double.isFinite(disciplina.getCargaHoraria()) || disciplina.getCargaHoraria() < 0) {
                throw new IllegalStateException("Carga horaria invalida: " + disciplina.getNome());
            }
            int periodosAntes = disciplina.getNumPeriodos();
            int minimoAntes = disciplina.getMinPeriodosJuntos();
            int maximoAntes = disciplina.getMaxPeriodosJuntos();
            var configuracaoAntes = disciplina.getTamanhoGrade();
            disciplina.setTamanhoGrade(configuracao);

            // Preenche somente limites ausentes; valores cadastrados permanecem como requisito.
            if (minimoAntes <= 0) {
                disciplina.setMinPeriodosJuntos(1);
            }
            if (maximoAntes <= 0) {
                disciplina.setMaxPeriodosJuntos(Math.max(disciplina.getMinPeriodosJuntos(),
                        Math.min(2, configuracao.getQtdePeriodosDia())));
            }
            try {
                blocos.calcularBlocos(disciplina, configuracao);
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new IllegalStateException("Revise os limites de blocos de " + disciplina.getNome()
                        + ": " + e.getMessage(), e);
            }
            if (periodosAntes != disciplina.getNumPeriodos()
                    || minimoAntes != disciplina.getMinPeriodosJuntos()
                    || maximoAntes != disciplina.getMaxPeriodosJuntos()
                    || configuracaoAntes == null
                    || !Objects.equals(configuracaoAntes.getId(), configuracao.getId())) {
                disciplinas.salvarDisciplina(disciplina);
                disciplinasAlteradas++;
                log.info("Disciplina {} adequada: periodos {}, blocos minimo={}, maximo={}",
                        disciplina.getNome(), disciplina.getNumPeriodos(),
                        disciplina.getMinPeriodosJuntos(), disciplina.getMaxPeriodosJuntos());
            }
        }

        int professoresAlterados = 0;
        for (var professor : professores.listarProfessores()) {
            Boolean[][][] anterior = professor.getDisponibilidadeHoraria();
            if (anterior != null && anterior.length != configuracao.getQtdeTurnos()) {
                log.warn("Disponibilidade de {} tem {} turnos, mas a configuracao tem {}. Revise o cadastro; matriz preservada.",
                        professor.getNome(), anterior.length, configuracao.getQtdeTurnos());
                continue;
            }
            Boolean[][][] ajustada = completarDisponibilidade(anterior,
                    configuracao.getQtdeTurnos(), configuracao.getQtdePeriodosDia(),
                    configuracao.getQtdeDiasSemana());
            if (!Arrays.deepEquals(anterior, ajustada)) {
                professor.setDisponibilidadeHoraria(ajustada);
                professores.salvarProfessor(professor);
                professoresAlterados++;
            }
        }

        var turmasCadastradas = turmas.listarTurmas();
        for (var turma : turmasCadastradas) {
            if (turma.getTamanhoGrade() == null
                    || !Objects.equals(turma.getTamanhoGrade().getId(), configuracao.getId())) {
                turma.setTamanhoGrade(configuracao);
                turmas.salvarTurma(turma);
            }
        }
        log.info("Adequacao concluida: {} disciplinas e {} professores atualizados; {} turmas verificadas.",
                disciplinasAlteradas, professoresAlterados, turmasCadastradas.size());

        try {
            PreparacaoOtimizacao.preparar(otimizacao.montarProblema(turmasCadastradas, configuracao));
            log.info("Validacoes iniciais da grade aprovadas.");
        } catch (DiagnosticoOtimizacaoException e) {
            // Mantem correcoes cadastrais e registra impedimentos que precisam de decisao humana.
            log.warn("Os dados foram adequados, mas ainda ha pendencias para gerar a grade:\n{}", e.getMessage());
        }
    }

    static Boolean[][][] completarDisponibilidade(
            Boolean[][][] anterior, int turnos, int periodos, int dias) {
        Boolean[][][] resultado = new Boolean[turnos][][];
        for (int turno = 0; turno < turnos; turno++) {
            Boolean[][] turnoAnterior = anterior == null ? null : anterior[turno];
            int totalPeriodos = Math.max(periodos, turnoAnterior == null ? 0 : turnoAnterior.length);
            resultado[turno] = new Boolean[totalPeriodos][];
            for (int periodo = 0; periodo < totalPeriodos; periodo++) {
                Boolean[] periodoAnterior = turnoAnterior == null || periodo >= turnoAnterior.length
                        ? null : turnoAnterior[periodo];
                int totalDias = Math.max(dias, periodoAnterior == null ? 0 : periodoAnterior.length);
                resultado[turno][periodo] = new Boolean[totalDias];
                for (int dia = 0; dia < totalDias; dia++) {
                    resultado[turno][periodo][dia] = periodoAnterior != null && dia < periodoAnterior.length
                            && Boolean.TRUE.equals(periodoAnterior[dia]);
                }
            }
        }
        return resultado;
    }
}
