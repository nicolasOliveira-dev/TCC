package com.example.EduGrid.solver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.solver.domain.AulaPlanejamento;
import com.example.EduGrid.solver.domain.CandidatoSincronizacao;
import com.example.EduGrid.solver.domain.GrupoSincronizacao;

class IdentificacaoSincronizacaoServiceTest {

    private final IdentificacaoSincronizacaoService service = new IdentificacaoSincronizacaoService();

    @Test
    void deveIdentificarCandidatosParaDisciplinasTecnicasDoMesmoCursoEmAnosDiferentes() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(10);

        Disciplina disciplinaTecnica1 = criarDisciplina(1, "Eletronica", "tecnica");
        Disciplina disciplinaTecnica2 = criarDisciplina(2, "Robotica", "tecnica");

        Turma turmaAno1 = new Turma(1, "Turma 1", List.of(disciplinaTecnica1), curso, null, 2024);
        Turma turmaAno2 = new Turma(2, "Turma 2", List.of(disciplinaTecnica2), curso, null, 2025);

        AulaPlanejamento aula1 = new AulaPlanejamento("A1", turmaAno1, disciplinaTecnica1, 2, null, null);
        AulaPlanejamento aula2 = new AulaPlanejamento("A2", turmaAno2, disciplinaTecnica2, 2, null, null);

        List<CandidatoSincronizacao> candidatos = service.identificarCandidatos(List.of(aula1, aula2));

        assertEquals(1, candidatos.size());
        assertEquals(aula1, candidatos.get(0).aula1());
        assertEquals(aula2, candidatos.get(0).aula2());
    }

    @Test
    void deveIgnorarDisciplinasNaoTecnicas() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(20);

        Disciplina mesmaDisciplina = criarDisciplina(3, "Historia", "comum");

        Turma turmaAno1 = new Turma(1, "Turma 1", List.of(mesmaDisciplina), curso, null, 2024);
        Turma turmaAno2 = new Turma(2, "Turma 2", List.of(mesmaDisciplina), curso, null, 2025);

        AulaPlanejamento aula1 = new AulaPlanejamento("A1", turmaAno1, mesmaDisciplina, 2, null, null);
        AulaPlanejamento aula2 = new AulaPlanejamento("A2", turmaAno2, mesmaDisciplina, 2, null, null);

        assertTrue(service.identificarCandidatos(List.of(aula1, aula2)).isEmpty());
    }

    @Test
    void deveIgnorarMesmasTurmasOuMesmoAno() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(30);

        Disciplina disciplinaTecnica = criarDisciplina(4, "Quimica", "tecnica");

        Turma turmaAno1A = new Turma(1, "Turma 1", List.of(disciplinaTecnica), curso, null, 2024);
        Turma turmaAno1B = new Turma(2, "Turma 2", List.of(disciplinaTecnica), curso, null, 2024);

        AulaPlanejamento aula1 = new AulaPlanejamento("A1", turmaAno1A, disciplinaTecnica, 2, null, null);
        AulaPlanejamento aula2 = new AulaPlanejamento("A2", turmaAno1B, disciplinaTecnica, 2, null, null);

        assertTrue(service.identificarCandidatos(List.of(aula1, aula2)).isEmpty());
    }

    @Test
    void deveIgnorarCursosDiferentesOuTurnosDiferentes() {
        Curso curso1 = new Curso("Curso A", "Regular", "matutino");
        curso1.setId(40);
        Curso curso2 = new Curso("Curso B", "Regular", "vespertino");
        curso2.setId(41);

        Disciplina disciplinaTecnica = criarDisciplina(5, "Fisica", "tecnica");

        Turma turma1 = new Turma(1, "Turma 1", List.of(disciplinaTecnica), curso1, null, 2024);
        Turma turma2 = new Turma(2, "Turma 2", List.of(disciplinaTecnica), curso2, null, 2025);

        AulaPlanejamento aula1 = new AulaPlanejamento("A1", turma1, disciplinaTecnica, 2, null, null);
        AulaPlanejamento aula2 = new AulaPlanejamento("A2", turma2, disciplinaTecnica, 2, null, null);

        assertTrue(service.identificarCandidatos(List.of(aula1, aula2)).isEmpty());
    }

    @Test
    void deveAgruparBlocosDeQuatroPeriodosComDoisBlocosDeDois() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(50);
        TamanhoGrade grade = new TamanhoGrade(1, 5, 5);

        Disciplina disciplina = criarDisciplina(6, "Eletronica", "tecnica");
        Turma turmaAno1 = new Turma(1, "Turma 1", List.of(disciplina), curso, grade, 2024);
        Turma turmaAno2 = new Turma(2, "Turma 2", List.of(disciplina), curso, grade, 2025);

        AulaPlanejamento bloco4 = new AulaPlanejamento("B4", turmaAno1, disciplina, 4, null, null);
        AulaPlanejamento bloco2a = new AulaPlanejamento("B2A", turmaAno2, disciplina, 2, null, null);
        AulaPlanejamento bloco2b = new AulaPlanejamento("B2B", turmaAno2, disciplina, 2, null, null);

        List<GrupoSincronizacao> grupos = service.identificarGruposSincronizacao(List.of(bloco4, bloco2a, bloco2b));

        assertEquals(1, grupos.size());
        assertEquals(4, grupos.get(0).totalPeriodos());
        assertEquals(2, grupos.get(0).blocosPorTurma().get(turmaAno2.getId()).size());
    }

    @Test
    void deveAgruparBlocosDeTresEDeDoisQuandoHaCapacidadeNaGrade() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(60);
        TamanhoGrade grade = new TamanhoGrade(1, 6, 5);

        Disciplina disciplina = criarDisciplina(7, "Robotica", "tecnica");
        Turma turmaAno1 = new Turma(1, "Turma 1", List.of(disciplina), curso, grade, 2024);
        Turma turmaAno2 = new Turma(2, "Turma 2", List.of(disciplina), curso, grade, 2025);

        AulaPlanejamento bloco3a = new AulaPlanejamento("T3A", turmaAno1, disciplina, 3, null, null);
        AulaPlanejamento bloco3b = new AulaPlanejamento("T3B", turmaAno1, disciplina, 3, null, null);
        AulaPlanejamento bloco2a = new AulaPlanejamento("T2A", turmaAno2, disciplina, 2, null, null);
        AulaPlanejamento bloco2b = new AulaPlanejamento("T2B", turmaAno2, disciplina, 2, null, null);
        AulaPlanejamento bloco2c = new AulaPlanejamento("T2C", turmaAno2, disciplina, 2, null, null);

        List<GrupoSincronizacao> grupos = service.identificarGruposSincronizacao(List.of(bloco3a, bloco3b, bloco2a, bloco2b, bloco2c));

        assertEquals(1, grupos.size());
        assertEquals(6, grupos.get(0).totalPeriodos());
    }

    @Test
    void deveIgnorarGruposQuandoTurmasPertencemAoMesmoAno() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(70);
        TamanhoGrade grade = new TamanhoGrade(1, 5, 5);

        Disciplina disciplina = criarDisciplina(8, "Quimica", "tecnica");
        Turma turmaAno1A = new Turma(1, "Turma 1", List.of(disciplina), curso, grade, 2024);
        Turma turmaAno1B = new Turma(2, "Turma 2", List.of(disciplina), curso, grade, 2024);

        AulaPlanejamento bloco4A = new AulaPlanejamento("Q4A", turmaAno1A, disciplina, 4, null, null);
        AulaPlanejamento bloco2A = new AulaPlanejamento("Q2A", turmaAno1B, disciplina, 2, null, null);
        AulaPlanejamento bloco2B = new AulaPlanejamento("Q2B", turmaAno1B, disciplina, 2, null, null);

        assertTrue(service.identificarGruposSincronizacao(List.of(bloco4A, bloco2A, bloco2B)).isEmpty());
    }

    @Test
    void deveIgnorarGruposQuandoCursoOuTurnoSaoDiferentes() {
        Curso curso1 = new Curso("Curso A", "Regular", "matutino");
        curso1.setId(80);
        Curso curso2 = new Curso("Curso B", "Regular", "vespertino");
        curso2.setId(81);
        TamanhoGrade grade = new TamanhoGrade(1, 5, 5);

        Disciplina disciplina = criarDisciplina(9, "Fisica", "tecnica");
        Turma turmaAno1 = new Turma(1, "Turma 1", List.of(disciplina), curso1, grade, 2024);
        Turma turmaAno2 = new Turma(2, "Turma 2", List.of(disciplina), curso2, grade, 2025);

        AulaPlanejamento bloco4A = new AulaPlanejamento("F4A", turmaAno1, disciplina, 4, null, null);
        AulaPlanejamento bloco2A = new AulaPlanejamento("F2A", turmaAno2, disciplina, 2, null, null);
        AulaPlanejamento bloco2B = new AulaPlanejamento("F2B", turmaAno2, disciplina, 2, null, null);

        assertTrue(service.identificarGruposSincronizacao(List.of(bloco4A, bloco2A, bloco2B)).isEmpty());
    }

    @Test
    void deveEscolherApenasUmGrupoQuandoUmaTurmaParticipaDeDiversasCombinacoes() {
        Curso curso = new Curso("Curso A", "Regular", "matutino");
        curso.setId(90);
        TamanhoGrade grade = new TamanhoGrade(1, 6, 5);

        Disciplina disciplina = criarDisciplina(10, "Algoritmos", "tecnica");
        Turma turmaAno1 = new Turma(1, "Turma 1", List.of(disciplina), curso, grade, 2024);
        Turma turmaAno2 = new Turma(2, "Turma 2", List.of(disciplina), curso, grade, 2025);

        AulaPlanejamento bloco4 = new AulaPlanejamento("G4", turmaAno1, disciplina, 4, null, null);
        AulaPlanejamento bloco2A = new AulaPlanejamento("G2A", turmaAno2, disciplina, 2, null, null);
        AulaPlanejamento bloco2B = new AulaPlanejamento("G2B", turmaAno2, disciplina, 2, null, null);
        AulaPlanejamento bloco3 = new AulaPlanejamento("G3", turmaAno2, disciplina, 3, null, null);
        AulaPlanejamento bloco1 = new AulaPlanejamento("G1", turmaAno2, disciplina, 1, null, null);

        List<GrupoSincronizacao> grupos = service.identificarGruposSincronizacao(
                List.of(bloco4, bloco2A, bloco2B, bloco3, bloco1));

        assertEquals(1, grupos.size());
        assertEquals(4, grupos.get(0).totalPeriodos());
    }

    private Disciplina criarDisciplina(int id, String nome, String tipoDisciplina) {
        Disciplina disciplina = new Disciplina(id, nome, "Geral", 2.0, null, null, tipoDisciplina);
        assertNotNull(disciplina);
        return disciplina;
    }
}
