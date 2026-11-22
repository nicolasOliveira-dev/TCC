package com.example.EduGrid.solver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;

import ai.timefold.solver.core.api.score.HardSoftScore;

class PersistenciaGradeServiceTest {

    @Test
    void persisteQuatroRegistrosParaBlocoDeQuatroPeriodos() {
        Fixture fixture = fixture("matutino", 4, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 4, fixture.professor(), 0, 0, 0);
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);
        List<Horario> salvos = capturarRegistrosSalvos(repository);

        service(repository).persistirResultado(grade);

        assertEquals(4, salvos.size());
        assertEquals(List.of(0, 1, 2, 3), salvos.stream()
                .map(Horario::getPeriodoDia)
                .sorted()
                .toList());
    }

    @Test
    void persisteSeisRegistrosParaBlocosDeQuatroMaisDois() {
        Fixture fixture = fixture("matutino", 6, 5);
        AulaPlanejamento blocoQuatro = aula("aula-1", fixture, 4, fixture.professor(), 0, 0, 0);
        AulaPlanejamento blocoDois = aula("aula-2", fixture, 2, fixture.professor(), 0, 1, 0);
        GradePlanejamento grade = grade(
                fixture,
                List.of(blocoQuatro, blocoDois),
                HardSoftScore.of(0, 0)
        );
        HorarioRepository repository = mock(HorarioRepository.class);
        List<Horario> salvos = capturarRegistrosSalvos(repository);

        service(repository).persistirResultado(grade);

        assertEquals(6, salvos.size());
    }

    @Test
    void atribuiOProfessorResolvidoAosRegistrosDeCadaPeriodo() {
        Fixture fixture = fixture("matutino", 4, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 4, fixture.professor(), 0, 0, 0);
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);
        List<Horario> salvos = capturarRegistrosSalvos(repository);

        service(repository).persistirResultado(grade);

        assertTrue(salvos.stream().allMatch(horario -> horario.getProfessor() == fixture.professor()));
    }

    @Test
    void preservaMarcacaoDeBlocosManuaisNaNovaVersao() {
        Fixture fixture = fixture("matutino", 2, 5);
        AulaPlanejamento aula = aula("aula-manual", fixture, 2, fixture.professor(), 0, 1, 0);
        aula.setBloqueadoManualmente(true);
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);
        List<Horario> salvos = capturarRegistrosSalvos(repository);

        service(repository).persistirResultado(grade);

        assertEquals(2, salvos.size());
        assertTrue(salvos.stream().allMatch(Horario::isManualmenteEditado));
    }

    @Test
    void persisteUmaGradeHistoricaSemSubstituirHorariosDeOutrasGrades() {
        Fixture fixture = fixture("matutino", 1, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 1, fixture.professor(), 0, 0, 0);
        GradePlanejamento planejamento = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        GradeHoraria gradeGerada = new GradeHoraria(
                List.of(fixture.turma()),
                fixture.turma().getTamanhoGrade()
        );
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        GradeHorariaRepository gradeRepository = mock(GradeHorariaRepository.class);
        when(gradeRepository.save(any(GradeHoraria.class))).thenAnswer(invocation -> {
            GradeHoraria salva = invocation.getArgument(0);
            salva.setId(42);
            return salva;
        });
        List<Horario> horariosSalvos = capturarRegistrosSalvos(horarioRepository);

        GradeHoraria resultado = new PersistenciaGradeService(
                horarioRepository,
                gradeRepository
        ).persistirResultado(planejamento, gradeGerada);

        assertEquals(42, resultado.getId());
        assertEquals(1, horariosSalvos.size());
        assertTrue(horariosSalvos.stream().allMatch(horario -> horario.getGradeHoraria() == resultado));
        verify(horarioRepository, never()).deleteByTurma(any(Turma.class));
    }

    @Test
    void salvaRelatorioHardComAGradeHistorica() {
        Fixture fixture = fixture("matutino", 1, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 1, fixture.professor(), 0, 0, 0);
        GradePlanejamento planejamento = grade(fixture, List.of(aula), HardSoftScore.of(-1, -2));
        GradeHoraria gradeGerada = new GradeHoraria(
                List.of(fixture.turma()), fixture.turma().getTamanhoGrade());
        HorarioRepository horarioRepository = mock(HorarioRepository.class);
        GradeHorariaRepository gradeRepository = mock(GradeHorariaRepository.class);
        when(gradeRepository.save(any(GradeHoraria.class))).thenAnswer(invocation -> {
            GradeHoraria salva = invocation.getArgument(0);
            salva.setId(43);
            return salva;
        });

        GradeHoraria salva = new PersistenciaGradeService(horarioRepository, gradeRepository)
                .persistirResultado(planejamento, gradeGerada, "Conflito de turma");

        assertEquals("Conflito de turma", salva.getRelatorioRestricoesHard());
        assertEquals(43, salva.getId());
    }

    @Test
    void persisteMelhorSolucaoMesmoComScoreHardNegativo() {
        Fixture fixture = fixture("matutino", 1, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 1, fixture.professor(), 0, 0, 0);
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(-1, 0));
        HorarioRepository repository = mock(HorarioRepository.class);

        service(repository).persistirResultado(grade);

        verify(repository).deleteByTurma(fixture.turma());
        verify(repository).saveAll(anyIterable());
    }

    @Test
    void rejeitaAulaSemHorarioAntesDeAlterarHorariosPersistidos() {
        Fixture fixture = fixture("matutino", 1, 5);
        AulaPlanejamento aula = new AulaPlanejamento(
                "aula-1",
                fixture.turma(),
                fixture.disciplina(),
                1,
                fixture.professor(),
                null
        );
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);

        assertThrows(IllegalStateException.class, () -> service(repository).persistirResultado(grade));

        verify(repository, never()).deleteByTurma(any(Turma.class));
        verify(repository, never()).saveAll(anyIterable());
    }

    @Test
    void erroAoSalvarFazRollbackDaTransacao() {
        Fixture fixture = fixture("matutino", 1, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 1, fixture.professor(), 0, 0, 0);
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);
        when(repository.saveAll(anyIterable())).thenThrow(new IllegalStateException("Falha de gravação."));
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        ProxyFactory proxyFactory = new ProxyFactory(new PersistenciaGradeService(
                repository,
                mock(GradeHorariaRepository.class)
        ));
        proxyFactory.addAdvice(new TransactionInterceptor(
                transactionManager,
                new AnnotationTransactionAttributeSource()
        ));
        PersistenciaGradeService transactionalService =
                (PersistenciaGradeService) proxyFactory.getProxy();

        assertThrows(
                IllegalStateException.class,
                () -> transactionalService.persistirResultado(grade)
        );

        assertEquals(1, transactionManager.getRollbackCount());
        assertEquals(0, transactionManager.getCommitCount());
    }

    @Test
    void persistirDuasVezesSubstituiOsRegistrosSemDuplicar() {
        Fixture fixture = fixture("matutino", 4, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 4, fixture.professor(), 0, 0, 0);
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);
        List<Horario> persistidos = new ArrayList<>();
        doAnswer(invocation -> {
            Turma turma = invocation.getArgument(0);
            persistidos.removeIf(horario -> horario.getTurma().getId().equals(turma.getId()));
            return null;
        }).when(repository).deleteByTurma(any(Turma.class));
        doAnswer(invocation -> {
            Iterable<Horario> novos = invocation.getArgument(0);
            List<Horario> novosRegistros = StreamSupport.stream(novos.spliterator(), false).toList();
            persistidos.addAll(novosRegistros);
            return novosRegistros;
        }).when(repository).saveAll(anyIterable());
        PersistenciaGradeService persistenceService = service(repository);

        persistenceService.persistirResultado(grade);
        persistenceService.persistirResultado(grade);

        assertEquals(4, persistidos.size());
        verify(repository, org.mockito.Mockito.times(2)).deleteByTurma(fixture.turma());
    }

    @Test
    void rejeitaCargaHorariaIncompletaAntesDeAlterarHorariosPersistidos() {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, 5, 5);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(
                        disciplina(1, "Matemática", 6, tamanhoGrade),
                        disciplina(2, "Português", 4, tamanhoGrade),
                        disciplina(3, "Física", 4, tamanhoGrade)
                ),
                new Curso("Informática", "Técnico", "matutino"),
                tamanhoGrade,
                1
        );
        Professor professor = professor(1);
        List<AulaPlanejamento> aulas = List.of(
                new AulaPlanejamento("mat-1", turma, turma.getDisciplinas().get(0), 4, professor,
                        new HorarioPlanejamento(0, 0, 0)),
                new AulaPlanejamento("mat-2", turma, turma.getDisciplinas().get(0), 2, professor,
                        new HorarioPlanejamento(1, 0, 0)),
                new AulaPlanejamento("por-1", turma, turma.getDisciplinas().get(1), 4, professor,
                        new HorarioPlanejamento(2, 0, 0)),
                new AulaPlanejamento("fis-1", turma, turma.getDisciplinas().get(2), 2, professor,
                        new HorarioPlanejamento(3, 0, 0))
        );
        GradePlanejamento grade = new GradePlanejamento(
                aulas,
                List.of(),
                List.of(professor),
                HardSoftScore.of(0, 0)
        );
        grade.setTurmas(List.of(turma));
        HorarioRepository repository = mock(HorarioRepository.class);

        assertThrows(IllegalStateException.class, () -> service(repository).persistirResultado(grade));

        verify(repository, never()).deleteByTurma(any(Turma.class));
        verify(repository, never()).saveAll(anyIterable());
    }

    @Test
    void turnoNoturnoEhValidoMesmoComUmaQuantidadeDeTurno() {
        Fixture fixture = fixture("noturno", 1, 5);
        AulaPlanejamento aula = aula("aula-1", fixture, 1, fixture.professor(), 2, 0, 0);
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);
        List<Horario> salvos = capturarRegistrosSalvos(repository);

        service(repository).persistirResultado(grade);

        assertEquals(1, salvos.size());
        assertEquals(2, salvos.get(0).getTurno());
    }

    @Test
    void rejeitaDisciplinaQueNaoPertenceATurma() {
        Fixture fixture = fixture("matutino", 1, 5);
        Disciplina indevida = disciplina(2, "Física", 1, fixture.turma().getTamanhoGrade());
        AulaPlanejamento aula = new AulaPlanejamento(
                "aula-indevida",
                fixture.turma(),
                indevida,
                1,
                fixture.professor(),
                new HorarioPlanejamento(0, 0, 0)
        );
        GradePlanejamento grade = grade(fixture, List.of(aula), HardSoftScore.of(0, 0));
        HorarioRepository repository = mock(HorarioRepository.class);

        assertThrows(IllegalStateException.class, () -> service(repository).persistirResultado(grade));

        verify(repository, never()).deleteByTurma(any(Turma.class));
        verify(repository, never()).saveAll(anyIterable());
    }

    private PersistenciaGradeService service(HorarioRepository repository) {
        return new PersistenciaGradeService(repository, mock(GradeHorariaRepository.class));
    }

    private List<Horario> capturarRegistrosSalvos(HorarioRepository repository) {
        List<Horario> salvos = new ArrayList<>();
        doAnswer(invocation -> {
            Iterable<Horario> horarios = invocation.getArgument(0);
            List<Horario> registros = StreamSupport.stream(horarios.spliterator(), false).toList();
            salvos.addAll(registros);
            return registros;
        }).when(repository).saveAll(anyIterable());
        return salvos;
    }

    private GradePlanejamento grade(
            Fixture fixture,
            List<AulaPlanejamento> aulas,
            HardSoftScore score) {
        GradePlanejamento grade = new GradePlanejamento(aulas, List.of(), List.of(fixture.professor()), score);
        grade.setTurmas(List.of(fixture.turma()));
        return grade;
    }

    private Fixture fixture(String turno, int periodosObrigatorios, int periodosPorDia) {
        TamanhoGrade tamanhoGrade = new TamanhoGrade(1, periodosPorDia, 5);
        Disciplina disciplina = disciplina(1, "Disciplina de teste", periodosObrigatorios, tamanhoGrade);
        Turma turma = new Turma(
                1,
                "INFO1",
                List.of(disciplina),
                new Curso("Informática", "Técnico", turno),
                tamanhoGrade,
                1
        );
        return new Fixture(turma, disciplina, professor(1));
    }

    private Disciplina disciplina(
            int id,
            String nome,
            int periodosObrigatorios,
            TamanhoGrade tamanhoGrade) {
        Disciplina disciplina = new Disciplina(
                id,
                nome,
                "Geral",
                periodosObrigatorios * tamanhoGrade.getTamanhoPeriodo() / 60.0,
                tamanhoGrade,
                null,
                "comum"
        );
        disciplina.setMinPeriodosJuntos(1);
        disciplina.setMaxPeriodosJuntos(4);
        return disciplina;
    }

    private Professor professor(int id) {
        return new Professor(
                id,
                "Professor " + id,
                "Geral",
                8,
                List.of(),
                new Boolean[][][]{{{true, true, true, true, true}}},
                List.of()
        );
    }

    private AulaPlanejamento aula(
            String id,
            Fixture fixture,
            int duracao,
            Professor professor,
            int turno,
            int dia,
            int periodoInicial) {
        return new AulaPlanejamento(
                id,
                fixture.turma(),
                fixture.disciplina(),
                duracao,
                professor,
                new HorarioPlanejamento(dia, periodoInicial, turno)
        );
    }

    private record Fixture(Turma turma, Disciplina disciplina, Professor professor) {
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private int rollbackCount;
        private int commitCount;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbackCount++;
        }

        int getRollbackCount() {
            return rollbackCount;
        }

        int getCommitCount() {
            return commitCount;
        }
    }
}
