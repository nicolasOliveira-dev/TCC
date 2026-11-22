package com.example.EduGrid.solver.constraint;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.GradePlanejamento;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;
import com.example.EduGrid.solver.domain.VinculoSincronizacao;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;

class GradeConstraintProviderTest {

    private final GradeConstraintProvider provider = new GradeConstraintProvider();
    private final ConstraintVerifier<GradeConstraintProvider, GradePlanejamento> constraintVerifier
            = ConstraintVerifier.build(provider, GradePlanejamento.class, AulaPlanejamento.class);

    private AulaPlanejamento criarAula(String id, int inicio, int duracao) {
        return criarAula(id, inicio, duracao, null, null);
    }

    private AulaPlanejamento criarAula(
            String id, int inicio, int duracao, Professor professor, Turma turma) {
        AulaPlanejamento aula = new AulaPlanejamento();
        aula.setId(id);
        aula.setDuracaoPeriodos(duracao);
        aula.setHorarioInicial(new HorarioPlanejamento(0, inicio, 0));
        aula.setProfessor(professor);
        aula.setTurma(turma);
        return aula;
    }

    private Disciplina criarDisciplinaSincronizada(int id, String tipo) {
        return new Disciplina(id, "Disciplina " + id, "Geral", 2.0, null, null, tipo);
    }

    private Turma criarTurmaSincronizada(
            int id, int ano, String turno, Disciplina disciplina) {
        Curso curso = new Curso("Curso " + turno, "Regular", turno);
        curso.setId(1);
        return new Turma(
                id,
                "Turma " + id,
                List.of(disciplina),
                curso,
                new TamanhoGrade(1, 5, 5),
                ano
        );
    }

    private AulaPlanejamento criarAulaSincronizada(
            String id, Turma turma, Disciplina disciplina, int dia, int periodo, int turno, int duracao) {
        AulaPlanejamento aula = criarAula(id, periodo, duracao, new Professor(), turma);
        aula.setDisciplina(disciplina);
        aula.setHorarioInicial(new HorarioPlanejamento(dia, periodo, turno));
        return aula;
    }

    private AulaPlanejamento criarAulaQuarta(
            String id, int turno, int periodo, int duracao, int periodosPorDia) {
        Turma turma = new Turma();
        turma.setTamanhoGrade(new TamanhoGrade(3, periodosPorDia, 5));
        AulaPlanejamento aula = criarAula(id, periodo, duracao, new Professor(), turma);
        aula.setHorarioInicial(new HorarioPlanejamento(2, periodo, turno));
        return aula;
    }

    @Test
    void penalizaOsDoisUltimosPeriodosDeQuartaNosTurnosDaManhaETarde() {
        AulaPlanejamento matutino = criarAulaQuarta("manha", 0, 4, 2, 6);
        AulaPlanejamento vespertino = criarAulaQuarta("tarde", 1, 4, 2, 6);

        constraintVerifier.verifyThat(GradeConstraintProvider::ultimosPeriodosQuartaFeira)
                .given(matutino, vespertino)
                .penalizesBy(2);
    }

    @Test
    void naoPenalizaOutrosDiasTurnoNoturnoNemPeriodosAnteriores() {
        AulaPlanejamento periodoPermitido = criarAulaQuarta("permitido", 0, 3, 1, 6);
        AulaPlanejamento turnoNoturno = criarAulaQuarta("noturno", 2, 4, 2, 6);
        AulaPlanejamento outroDia = criarAulaQuarta("outro-dia", 1, 4, 2, 6);
        outroDia.setHorarioInicial(new HorarioPlanejamento(1, 4, 1));

        constraintVerifier.verifyThat(GradeConstraintProvider::ultimosPeriodosQuartaFeira)
                .given(periodoPermitido, turnoNoturno, outroDia)
                .penalizesBy(0);
    }

    @Test
    void detectaBlocoQueComecaAntesMasAlcancaPeriodoProibido() {
        AulaPlanejamento bloco = criarAulaQuarta("bloco", 0, 2, 3, 5);

        constraintVerifier.verifyThat(GradeConstraintProvider::ultimosPeriodosQuartaFeira)
                .given(bloco)
                .penalizesBy(1);
    }

    @Test
    void adaptaOsDoisUltimosPeriodosQuandoAConfiguracaoMuda() {
        AulaPlanejamento bloco = criarAulaQuarta("bloco-dinamico", 1, 5, 2, 8);

        constraintVerifier.verifyThat(GradeConstraintProvider::ultimosPeriodosQuartaFeira)
                .given(bloco)
                .penalizesBy(1);
    }

    @Test
    void deveIdentificarSobreposicao() {
        AulaPlanejamento aula1 = criarAula("1", 0, 3);
        AulaPlanejamento aula2 = criarAula("2", 2, 2);

        assertTrue(provider.horariosSobrepostos(aula1, aula2));
    }

    @Test
    void naoDeveConsiderarAulasConsecutivasComoConflito() {
        AulaPlanejamento aula1 = criarAula("1", 0, 2);
        AulaPlanejamento aula2 = criarAula("2", 2, 2);

        assertFalse(provider.horariosSobrepostos(aula1, aula2));
    }

    @Test
    void naoDeveConsiderarDiasDiferentesComoConflito() {
        AulaPlanejamento aula1 = criarAula("1", 0, 3);
        AulaPlanejamento aula2 = criarAula("2", 0, 3);
        aula2.setHorarioInicial(new HorarioPlanejamento(1, 0, 0));

        assertFalse(provider.horariosSobrepostos(aula1, aula2));
    }

    @Test
    void devePenalizarConflitoDeProfessor() {
        Professor professor = new Professor();
        Professor outroProfessor = new Professor();
        AulaPlanejamento aula1 = criarAula("1", 0, 3, professor, null);
        AulaPlanejamento aula2 = criarAula("2", 2, 2, professor, null);
        AulaPlanejamento aulaSemConflito = criarAula("3", 1, 2, outroProfessor, null);

        constraintVerifier.verifyThat(GradeConstraintProvider::conflitoProfessor)
                .given(aula1, aula2, aulaSemConflito)
                .penalizesBy(1);
    }

    @Test
    void devePenalizarConflitoDeTurma() {
        Turma turma = new Turma();
        turma.setId(1);
        Turma outraTurma = new Turma();
        outraTurma.setId(2);
        AulaPlanejamento aula1 = criarAula("1", 0, 3, new Professor(), turma);
        AulaPlanejamento aula2 = criarAula("2", 2, 2, new Professor(), turma);
        AulaPlanejamento aulaSemConflito = criarAula("3", 1, 2, new Professor(), outraTurma);

        constraintVerifier.verifyThat(GradeConstraintProvider::conflitoTurma)
                .given(aula1, aula2, aulaSemConflito)
                .penalizesBy(1);
    }

    @Test
    void devePenalizarProfessorIndisponivelNoHorario() {
        Professor professor = new Professor();
        professor.setDisponibilidadeHoraria(new Boolean[][][]{{{false}}});
        AulaPlanejamento aula = criarAula("1", 0, 1, professor, null);

        constraintVerifier.verifyThat(GradeConstraintProvider::professorIndisponivel)
                .given(aula)
                .penalizesBy(1);
    }

    @Test
    void naoDevePenalizarProfessorDisponivelNoHorario() {
        Professor professor = new Professor();
        professor.setDisponibilidadeHoraria(new Boolean[][][]{{{true}}});
        AulaPlanejamento aula = criarAula("1", 0, 1, professor, null);

        constraintVerifier.verifyThat(GradeConstraintProvider::professorIndisponivel)
                .given(aula)
                .penalizesBy(0);
    }

    @Test
    void devePenalizarProfessorSemNenhumDiaLivre() {
        Professor professor = new Professor();
        Turma turma = new Turma();
        turma.setTamanhoGrade(new TamanhoGrade(1, 5, 5));

        List<AulaPlanejamento> aulas = new ArrayList<>();
        for (int dia = 0; dia < 5; dia++) {
            AulaPlanejamento aula = criarAula("dia-" + dia, 0, 1, professor, turma);
            aula.setHorarioInicial(new HorarioPlanejamento(dia, 0, 0));
            aulas.add(aula);
        }

        constraintVerifier.verifyThat(GradeConstraintProvider::professorSemDiaPlanejamento)
                .given(aulas.toArray())
                .penalizesBy(1);
    }

    @Test
    void naoDevePenalizarProfessorComDiaLivreMesmoComVariasAulasNoMesmoDia() {
        Professor professor = new Professor();
        Turma turma = new Turma();
        turma.setTamanhoGrade(new TamanhoGrade(1, 5, 5));

        AulaPlanejamento aula1 = criarAula("1", 0, 1, professor, turma);
        AulaPlanejamento aula2 = criarAula("2", 1, 1, professor, turma);
        AulaPlanejamento aula3 = criarAula("3", 0, 1, professor, turma);
        aula2.setHorarioInicial(new HorarioPlanejamento(1, 1, 0));

        constraintVerifier.verifyThat(GradeConstraintProvider::professorSemDiaPlanejamento)
                .given(aula1, aula2, aula3)
                .penalizesBy(0);
    }

    @Test
    void devePenalizarAulaEmDiaDePlanejamentoPreferido() {
        Professor professor = new Professor();
        professor.setIntencaoDiaPlanejamento(List.of("Segunda-feira"));
        AulaPlanejamento aula = criarAula("1", 0, 1, professor, null);

        constraintVerifier.verifyThat(GradeConstraintProvider::preferenciaDiaPlanejamento)
                .given(aula)
                .penalizesBy(1);
    }

    @Test
    void naoDevePenalizarAulaForaDoDiaDePlanejamentoPreferido() {
        Professor professor = new Professor();
        professor.setIntencaoDiaPlanejamento(List.of("Segunda-feira"));
        AulaPlanejamento aula = criarAula("1", 0, 1, professor, null);
        aula.setHorarioInicial(new HorarioPlanejamento(1, 0, 0));

        constraintVerifier.verifyThat(GradeConstraintProvider::preferenciaDiaPlanejamento)
                .given(aula)
                .penalizesBy(0);
    }

    @Test
    void devePenalizarMesmaDisciplinaTecnicaNoMesmoCursoAnoEmHorarioSobreposto() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(1);

        Disciplina disciplinaTecnica = new Disciplina(1, "Eletronica", "Geral", 2.0, null, null, "tecnica");
        Turma turma1 = new Turma(10, "Turma 1", List.of(disciplinaTecnica), curso, new TamanhoGrade(1, 5, 5), 2024);
        Turma turma2 = new Turma(11, "Turma 2", List.of(disciplinaTecnica), curso, new TamanhoGrade(1, 5, 5), 2024);

        AulaPlanejamento aula1 = criarAula("A1", 0, 2, new Professor(), turma1);
        aula1.setDisciplina(disciplinaTecnica);
        AulaPlanejamento aula2 = criarAula("A2", 1, 2, new Professor(), turma2);
        aula2.setDisciplina(disciplinaTecnica);

        constraintVerifier.verifyThat(GradeConstraintProvider::mesmaDisciplinaTecnicaMesmoAno)
                .given(aula1, aula2)
                .penalizesBy(1);
    }

    @Test
    void naoDevePenalizarMesmaDisciplinaTecnicaEmCursosOuAnosDiferentes() {
        Curso curso1 = new Curso("Curso A", "Regular", "matutino");
        curso1.setId(1);
        Curso curso2 = new Curso("Curso B", "Regular", "matutino");
        curso2.setId(2);

        Disciplina disciplinaTecnica = new Disciplina(1, "Eletronica", "Geral", 2.0, null, null, "tecnica");
        Turma turma1 = new Turma(10, "Turma 1", List.of(disciplinaTecnica), curso1, new TamanhoGrade(1, 5, 5), 2024);
        Turma turma2 = new Turma(11, "Turma 2", List.of(disciplinaTecnica), curso2, new TamanhoGrade(1, 5, 5), 2025);

        AulaPlanejamento aula1 = criarAula("A1", 0, 2, new Professor(), turma1);
        aula1.setDisciplina(disciplinaTecnica);
        AulaPlanejamento aula2 = criarAula("A2", 1, 2, new Professor(), turma2);
        aula2.setDisciplina(disciplinaTecnica);

        constraintVerifier.verifyThat(GradeConstraintProvider::mesmaDisciplinaTecnicaMesmoAno)
                .given(aula1, aula2)
                .penalizesBy(0);
    }

    @Test
    void naoDevePenalizarGrupoTecnicoTotalmenteSincronizadoComBlocosDeDuracoesDiferentes() {
        Turma turma1 = new Turma();
        turma1.setId(1);
        Turma turma2 = new Turma();
        turma2.setId(2);

        AulaPlanejamento algoritmos = criarAula("algoritmos", 0, 4, new Professor(), turma1);
        AulaPlanejamento lp = criarAula("lp", 0, 2, new Professor(), turma2);
        AulaPlanejamento bd = criarAula("bd", 2, 2, new Professor(), turma2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoDisciplinasTecnicas)
                .given(
                        algoritmos,
                        lp,
                        bd,
                        new VinculoSincronizacao("grupo-1", turma1.getId(), algoritmos.getId()),
                        new VinculoSincronizacao("grupo-1", turma2.getId(), lp.getId()),
                        new VinculoSincronizacao("grupo-1", turma2.getId(), bd.getId())
                )
                .penalizesBy(0);
    }

    @Test
    void devePenalizarUmPeriodoNaoSincronizadoNoGrupoTecnico() {
        Turma turma1 = new Turma();
        turma1.setId(1);
        Turma turma2 = new Turma();
        turma2.setId(2);

        AulaPlanejamento algoritmos = criarAula("algoritmos", 0, 4, new Professor(), turma1);
        AulaPlanejamento lp = criarAula("lp", 0, 2, new Professor(), turma2);
        AulaPlanejamento bd = criarAula("bd", 3, 2, new Professor(), turma2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoDisciplinasTecnicas)
                .given(
                        algoritmos,
                        lp,
                        bd,
                        new VinculoSincronizacao("grupo-1", turma1.getId(), algoritmos.getId()),
                        new VinculoSincronizacao("grupo-1", turma2.getId(), lp.getId()),
                        new VinculoSincronizacao("grupo-1", turma2.getId(), bd.getId())
                )
                .penalizesBy(10);
    }

    @Test
    void naoDeveContarPeriodosIguaisEmDiasDiferentesComoSincronizados() {
        Turma turma1 = new Turma();
        turma1.setId(1);
        Turma turma2 = new Turma();
        turma2.setId(2);

        AulaPlanejamento aula1 = criarAula("aula-1", 0, 2, new Professor(), turma1);
        AulaPlanejamento aula2 = criarAula("aula-2", 0, 2, new Professor(), turma2);
        aula2.setHorarioInicial(new HorarioPlanejamento(1, 0, 0));

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoDisciplinasTecnicas)
                .given(
                        aula1,
                        aula2,
                        new VinculoSincronizacao("grupo-1", turma1.getId(), aula1.getId()),
                        new VinculoSincronizacao("grupo-1", turma2.getId(), aula2.getId())
                )
                .penalizesBy(20);
    }

    @Test
    void naoDeveContarPeriodosIguaisEmTurnosDiferentesComoSincronizados() {
        Turma turma1 = new Turma();
        turma1.setId(1);
        Turma turma2 = new Turma();
        turma2.setId(2);

        AulaPlanejamento aula1 = criarAula("aula-1", 0, 2, new Professor(), turma1);
        AulaPlanejamento aula2 = criarAula("aula-2", 0, 2, new Professor(), turma2);
        aula2.setHorarioInicial(new HorarioPlanejamento(0, 0, 1));

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoDisciplinasTecnicas)
                .given(
                        aula1,
                        aula2,
                        new VinculoSincronizacao("grupo-1", turma1.getId(), aula1.getId()),
                        new VinculoSincronizacao("grupo-1", turma2.getId(), aula2.getId())
                )
                .penalizesBy(20);
    }

    @Test
    void deveContarPeriodosComunsATodasAsTurmasSemDuplicarCoincidenciasEntrePares() {
        Turma turma1 = new Turma();
        turma1.setId(1);
        Turma turma2 = new Turma();
        turma2.setId(2);
        Turma turma3 = new Turma();
        turma3.setId(3);

        AulaPlanejamento aula1 = criarAula("aula-1", 0, 2, new Professor(), turma1);
        AulaPlanejamento aula2 = criarAula("aula-2", 1, 2, new Professor(), turma2);
        AulaPlanejamento aula3 = criarAula("aula-3", 2, 2, new Professor(), turma3);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoDisciplinasTecnicas)
                .given(
                        aula1,
                        aula2,
                        aula3,
                        new VinculoSincronizacao("grupo-1", turma1.getId(), aula1.getId()),
                        new VinculoSincronizacao("grupo-1", turma2.getId(), aula2.getId()),
                        new VinculoSincronizacao("grupo-1", turma3.getId(), aula3.getId())
                )
                .penalizesBy(20);
    }

    @Test
    void devePenalizarNivelamentoNaMesmaDisciplinaAnoETurnoQuandoOsPeriodosDivergem() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 1, "matutino", nivelamento);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 2, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 0, 3, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2)
                .penalizesBy(2);
    }

    @Test
    void naoDevePenalizarNivelamentoSincronizadoNaMesmaDisciplinaAnoETurno() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 1, "matutino", nivelamento);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 2, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 0, 2, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2)
                .penalizesBy(0);
    }

    @Test
    void naoDeveSincronizarNivelamentoEntreAnosDiferentes() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 2, "matutino", nivelamento);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 0, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 0, 2, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2)
                .penalizesBy(0);
    }

    @Test
    void nivelamentoUsaTurnoDoCursoParaSepararGrupos() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turmaManha = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turmaTarde = criarTurmaSincronizada(2, 1, "vespertino", nivelamento);
        AulaPlanejamento aulaManha = criarAulaSincronizada(
                "nivel-manha", turmaManha, nivelamento, 0, 0, 0, 2);
        AulaPlanejamento aulaTarde = criarAulaSincronizada(
                "nivel-tarde", turmaTarde, nivelamento, 1, 2, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aulaManha, aulaTarde)
                .penalizesBy(0);
    }

    @Test
    void devePenalizarNivelamentoEmDiasDiferentesDentroDoMesmoTurno() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 1, "matutino", nivelamento);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 2, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 1, 2, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2)
                .penalizesBy(4);
    }

    @Test
    void deveCompararOTurnoDoHorarioDentroDoGrupoNivelamento() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 1, "matutino", nivelamento);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 2, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 0, 2, 1, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2)
                .penalizesBy(4);
    }

    @Test
    void devePenalizarEletivaEntreAnosDiferentesNoMesmoTurnoQuandoOsPeriodosDivergem() {
        Disciplina eletiva = criarDisciplinaSincronizada(2, "eletiva");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", eletiva);
        Turma turma2 = criarTurmaSincronizada(2, 3, "matutino", eletiva);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "eletiva-1", turma1, eletiva, 0, 0, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "eletiva-2", turma2, eletiva, 0, 2, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoEletivas)
                .given(aula1, aula2)
                .penalizesBy(4);
    }

    @Test
    void naoDevePenalizarEletivaSincronizadaEntreAnosDiferentesNoMesmoTurno() {
        Disciplina eletiva = criarDisciplinaSincronizada(2, "eletiva");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", eletiva);
        Turma turma2 = criarTurmaSincronizada(2, 3, "matutino", eletiva);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "eletiva-1", turma1, eletiva, 0, 0, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "eletiva-2", turma2, eletiva, 0, 0, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoEletivas)
                .given(aula1, aula2)
                .penalizesBy(0);
    }

    @Test
    void eletivaUsaTurnoDoCursoParaSepararGrupos() {
        Disciplina eletiva = criarDisciplinaSincronizada(2, "eletiva");
        Turma turmaManha = criarTurmaSincronizada(1, 1, "matutino", eletiva);
        Turma turmaTarde = criarTurmaSincronizada(2, 3, "vespertino", eletiva);
        AulaPlanejamento aulaManha = criarAulaSincronizada(
                "eletiva-manha", turmaManha, eletiva, 0, 0, 0, 2);
        AulaPlanejamento aulaTarde = criarAulaSincronizada(
                "eletiva-tarde", turmaTarde, eletiva, 1, 2, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoEletivas)
                .given(aulaManha, aulaTarde)
                .penalizesBy(0);
    }

    @Test
    void comparaConjuntoDePeriodosQuandoDisciplinaPossuiVariosBlocos() {
        Disciplina eletiva = criarDisciplinaSincronizada(2, "eletiva");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", eletiva);
        Turma turma2 = criarTurmaSincronizada(2, 2, "matutino", eletiva);

        AulaPlanejamento bloco1Turma1 = criarAulaSincronizada(
                "eletiva-1a", turma1, eletiva, 0, 0, 0, 2);
        AulaPlanejamento bloco2Turma1 = criarAulaSincronizada(
                "eletiva-1b", turma1, eletiva, 0, 2, 0, 2);
        AulaPlanejamento blocoTurma2 = criarAulaSincronizada(
                "eletiva-2", turma2, eletiva, 0, 0, 0, 4);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoEletivas)
                .given(bloco1Turma1, bloco2Turma1, blocoTurma2)
                .penalizesBy(0);
    }

    @Test
    void devePenalizarTresTurmasQuandoUmaNaoSincronizaComAsOutras() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 1, "matutino", nivelamento);
        Turma turma3 = criarTurmaSincronizada(3, 1, "matutino", nivelamento);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 0, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 0, 0, 0, 2);
        AulaPlanejamento aula3 = criarAulaSincronizada(
                "nivel-3", turma3, nivelamento, 0, 2, 0, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2, aula3)
                .penalizesBy(4);
    }

    @Test
    void sincronizacaoNaoDesativaConflitoDeProfessorEntreAulasIndependentes() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 1, "matutino", nivelamento);
        Professor professor = new Professor();
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 2, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 0, 2, 0, 2);
        aula1.setProfessor(professor);
        aula2.setProfessor(professor);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2)
                .penalizesBy(0);
        constraintVerifier.verifyThat(GradeConstraintProvider::conflitoProfessor)
                .given(aula1, aula2)
                .penalizesBy(1);
    }

    @Test
    void turnoIncompativelContinuaPenalizadoMesmoComTurmasNoMesmoGrupoDeNivelamento() {
        Disciplina nivelamento = criarDisciplinaSincronizada(1, "nivelamento");
        Turma turma1 = criarTurmaSincronizada(1, 1, "matutino", nivelamento);
        Turma turma2 = criarTurmaSincronizada(2, 1, "matutino", nivelamento);
        AulaPlanejamento aula1 = criarAulaSincronizada(
                "nivel-1", turma1, nivelamento, 0, 2, 0, 2);
        AulaPlanejamento aula2 = criarAulaSincronizada(
                "nivel-2", turma2, nivelamento, 0, 2, 1, 2);

        constraintVerifier.verifyThat(GradeConstraintProvider::sincronizacaoNivelamento)
                .given(aula1, aula2)
                .penalizesBy(4);
        constraintVerifier.verifyThat(GradeConstraintProvider::turnoIncompativel)
                .given(aula2)
                .penalizesBy(1);
    }

    @Test
    void professorPodeSerAlocadoEmMaisDeUmTurnoSeCadaAulaRespeitarOTurnoDaTurma() {
        Disciplina disciplinaManha = criarDisciplinaSincronizada(10, "comum");
        Disciplina disciplinaTarde = criarDisciplinaSincronizada(11, "comum");
        Turma turmaManha = criarTurmaSincronizada(10, 1, "matutino", disciplinaManha);
        Turma turmaTarde = criarTurmaSincronizada(11, 1, "vespertino", disciplinaTarde);
        Professor professor = new Professor();

        AulaPlanejamento aulaManha = criarAulaSincronizada(
                "professor-manha", turmaManha, disciplinaManha, 0, 0, 0, 1);
        AulaPlanejamento aulaTarde = criarAulaSincronizada(
                "professor-tarde", turmaTarde, disciplinaTarde, 0, 0, 1, 1);
        aulaManha.setProfessor(professor);
        aulaTarde.setProfessor(professor);

        constraintVerifier.verifyThat(GradeConstraintProvider::turnoIncompativel)
                .given(aulaManha, aulaTarde)
                .penalizesBy(0);
        constraintVerifier.verifyThat(GradeConstraintProvider::conflitoProfessor)
                .given(aulaManha, aulaTarde)
                .penalizesBy(0);
        constraintVerifier.verifyThat(GradeConstraintProvider::descansoEntreTurnos)
                .given(aulaManha, aulaTarde)
                .penalizesBy(0);
    }

    @Test
    void penalizaProfessorAssociadoAulaEmTurnoDiferenteDoTurnoDaTurma() {
        Disciplina disciplina = criarDisciplinaSincronizada(12, "comum");
        Turma turmaTarde = criarTurmaSincronizada(12, 1, "vespertino", disciplina);
        AulaPlanejamento aula = criarAulaSincronizada(
                "aula-tarde-no-turno-errado", turmaTarde, disciplina, 0, 0, 0, 1);

        constraintVerifier.verifyThat(GradeConstraintProvider::turnoIncompativel)
                .given(aula)
                .penalizesBy(1);
    }

    @Test
    void penalizaAulaNoturnaSeguidaDeAulaMatutinaNoDiaSeguinteDoMesmoProfessor() {
        Disciplina disciplinaNoite = criarDisciplinaSincronizada(13, "comum");
        Disciplina disciplinaManha = criarDisciplinaSincronizada(14, "comum");
        Turma turmaNoite = criarTurmaSincronizada(13, 1, "noturno", disciplinaNoite);
        Turma turmaManha = criarTurmaSincronizada(14, 1, "matutino", disciplinaManha);
        Professor professor = new Professor();

        AulaPlanejamento aulaNoite = criarAulaSincronizada(
                "professor-noite", turmaNoite, disciplinaNoite, 0, 4, 2, 1);
        AulaPlanejamento aulaManha = criarAulaSincronizada(
                "professor-manha-dia-seguinte", turmaManha, disciplinaManha, 1, 0, 0, 1);
        aulaNoite.setProfessor(professor);
        aulaManha.setProfessor(professor);

        constraintVerifier.verifyThat(GradeConstraintProvider::turnoIncompativel)
                .given(aulaNoite, aulaManha)
                .penalizesBy(0);
        constraintVerifier.verifyThat(GradeConstraintProvider::descansoEntreTurnos)
                .given(aulaNoite, aulaManha)
                .penalizesBy(1);
    }

    @Test
    void devePenalizarProfessorSemDisponibilidadeConfigurada() {
        Professor professor = new Professor();
        professor.setDisponibilidadeHoraria(null);

        AulaPlanejamento aula
                = criarAula("1", 0, 1, professor, null);

        constraintVerifier
                .verifyThat(GradeConstraintProvider::professorIndisponivel)
                .given(aula)
                .penalizesBy(1);
    }

    @Test
    void devePenalizarBlocoComPeriodoIndisponivel() {
        Professor professor = new Professor();

        professor.setDisponibilidadeHoraria(
                new Boolean[][][]{
                    {
                        {true},
                        {true},
                        {false},
                        {true}
                    }
                }
        );

        AulaPlanejamento aula
                = criarAula("1", 0, 4, professor, null);

        constraintVerifier
                .verifyThat(GradeConstraintProvider::professorIndisponivel)
                .given(aula)
                .penalizesBy(1);
    }

}
