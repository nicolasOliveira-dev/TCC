
package com.example.EduGrid.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.CursoRepository;
import com.example.EduGrid.repository.DisciplinaRepository;
import com.example.EduGrid.repository.ProfessorRepository;
import com.example.EduGrid.repository.TurmaRepository;

class ImportacaoServiceTest {

    private CursoRepository cursoRepository;
    private DisciplinaRepository disciplinaRepository;
    private TurmaRepository turmaRepository;
    private ProfessorRepository professorRepository;

    private ImportacaoService service;

    @BeforeEach
    void configurar() {
        cursoRepository = mock(CursoRepository.class);
        disciplinaRepository = mock(DisciplinaRepository.class);
        turmaRepository = mock(TurmaRepository.class);
        professorRepository = mock(ProfessorRepository.class);

        service = new ImportacaoService(
                cursoRepository,
                disciplinaRepository,
                turmaRepository,
                professorRepository
        );

        when(cursoRepository.save(any(Curso.class)))
                .thenAnswer(invocation -> {
                    Curso curso = invocation.getArgument(0);
                    curso.setId(1);
                    return curso;
                });

        when(disciplinaRepository.save(any(Disciplina.class)))
                .thenAnswer(invocation -> {
                    Disciplina disciplina = invocation.getArgument(0);
                    disciplina.setId(10);
                    return disciplina;
                });

        when(turmaRepository.save(any(Turma.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(professorRepository.save(any(Professor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private XSSFWorkbook planilhaBase() {
        XSSFWorkbook workbook = new XSSFWorkbook();

        adicionarAba(workbook, "Cursos",
                new String[]{"codigo", "nomeCurso", "tipoCurso", "turno"},
                new String[]{"C1", "Informática", "Técnico", "matutino"});

        adicionarAba(workbook, "Disciplinas",
                new String[]{"codigo", "nome", "areaAtuacao",
                        "cargaHoraria", "curso", "tipoDisciplina",
                        "minPeriodosJuntos", "maxPeriodosJuntos"},
                new String[]{"D1", "Algoritmos", "Informática",
                        "3", "C1", "tecnica", "2", "4"});

        adicionarAba(workbook, "Turmas",
                new String[]{"codigo", "nome", "curso", "ano", "disciplinas"},
                new String[]{"T1", "INFO1", "C1", "1", "D1"});

        adicionarAba(workbook, "Professores",
                new String[]{"codigo", "nome", "areaAtuacao",
                        "cargaHoraria", "disciplinasPossiveis"},
                new String[]{"P1", "Carlos", "Informática", "20", "D1"});

        return workbook;
    }

    private void adicionarAba(
            XSSFWorkbook workbook,
            String nome,
            String[] cabecalho,
            String[] dados) {

        Sheet sheet = workbook.createSheet(nome);
        preencherLinha(sheet.createRow(0), cabecalho);
        preencherLinha(sheet.createRow(1), dados);
    }

    private void preencherLinha(Row row, String[] valores) {
        for (int i = 0; i < valores.length; i++) {
            row.createCell(i).setCellValue(valores[i]);
        }
    }

    private MockMultipartFile arquivo(XSSFWorkbook workbook)
            throws IOException {

        try (workbook;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            workbook.write(output);

            return new MockMultipartFile(
                    "arquivo",
                    "importacao.xlsx",
                    "application/vnd.openxmlformats-officedocument."
                            + "spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }

    private void verificarNenhumaPersistencia() {
        verify(cursoRepository, never()).save(any());
        verify(disciplinaRepository, never()).save(any());
        verify(turmaRepository, never()).save(any());
        verify(professorRepository, never()).save(any());
    }

    @Test
    void deveImportarTodasAsEntidades() throws Exception {
        var resultado = service.importar(arquivo(planilhaBase()));

        assertEquals(1, resultado.cursos());
        assertEquals(1, resultado.disciplinas());
        assertEquals(1, resultado.turmas());
        assertEquals(1, resultado.professores());

        verify(cursoRepository).save(any(Curso.class));
        verify(disciplinaRepository).save(any(Disciplina.class));
        verify(turmaRepository).save(any(Turma.class));
        verify(professorRepository).save(any(Professor.class));
    }

    @Test
    void devePreservarRelacionamentosEntreEntidades() throws Exception {
        service.importar(arquivo(planilhaBase()));

        ArgumentCaptor<Disciplina> disciplinaCaptor =
                ArgumentCaptor.forClass(Disciplina.class);
        ArgumentCaptor<Turma> turmaCaptor =
                ArgumentCaptor.forClass(Turma.class);
        ArgumentCaptor<Professor> professorCaptor =
                ArgumentCaptor.forClass(Professor.class);

        verify(disciplinaRepository).save(disciplinaCaptor.capture());
        verify(turmaRepository).save(turmaCaptor.capture());
        verify(professorRepository).save(professorCaptor.capture());

        Disciplina disciplina = disciplinaCaptor.getValue();
        Turma turma = turmaCaptor.getValue();
        Professor professor = professorCaptor.getValue();

        assertEquals("Informática", disciplina.getCurso().getNomeCurso());
        assertEquals(2, disciplina.getMinPeriodosJuntos());
        assertEquals(4, disciplina.getMaxPeriodosJuntos());

        assertSame(disciplina, turma.getDisciplinas().get(0));
        assertSame(disciplina,
                professor.getDisciplinasPossiveis().get(0));

        assertEquals(1, turma.getAno());
        assertEquals(20, professor.getCargaHoraria());

        assertNull(professor.getDisponibilidadeHoraria());
        assertNull(professor.getIntencaoDiaPlanejamento());
    }

    @Test
    void deveRejeitarArquivoVazio() {
        MockMultipartFile vazio = new MockMultipartFile(
                "arquivo", "vazio.xlsx",
                "application/vnd.openxmlformats-officedocument."
                        + "spreadsheetml.sheet",
                new byte[0]
        );

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(vazio));

        assertEquals("Selecione uma planilha para importar.",
                erro.getMessage());

        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarExtensaoInvalida() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "dados.csv", "text/csv",
                "conteudo".getBytes()
        );

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo));

        assertEquals("O arquivo deve estar no formato .xlsx.",
                erro.getMessage());

        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarAbaAusente() throws Exception {
        XSSFWorkbook workbook = planilhaBase();
        workbook.removeSheetAt(
                workbook.getSheetIndex("Professores"));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo(workbook)));

        assertTrue(erro.getMessage().contains("Professores"));
        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarCodigoDuplicado() throws Exception {
        XSSFWorkbook workbook = planilhaBase();

        Sheet cursos = workbook.getSheet("Cursos");
        preencherLinha(cursos.createRow(2),
                new String[]{"C1", "Administração",
                        "Técnico", "vespertino"});

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo(workbook)));

        assertTrue(erro.getMessage().contains("Código duplicado"));
        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarCursoInexistente() throws Exception {
        XSSFWorkbook workbook = planilhaBase();

        workbook.getSheet("Disciplinas")
                .getRow(1).getCell(4).setCellValue("C999");

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo(workbook)));

        assertTrue(erro.getMessage().contains("Curso inexistente"));
        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarDisciplinaInexistenteNaTurma()
            throws Exception {

        XSSFWorkbook workbook = planilhaBase();

        workbook.getSheet("Turmas")
                .getRow(1).getCell(4).setCellValue("D999");

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo(workbook)));

        assertTrue(erro.getMessage().contains(
                "Disciplina inexistente"));
        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarCargaHorariaInvalida() throws Exception {
        XSSFWorkbook workbook = planilhaBase();

        workbook.getSheet("Disciplinas")
                .getRow(1).getCell(3).setCellValue("-2");

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo(workbook)));

        assertTrue(erro.getMessage().contains(
                "Carga horária ou limites de blocos inválidos"));
        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarProfessorJaCadastrado() throws Exception {
        Professor existente = new Professor();
        existente.setNome("Carlos");

        when(professorRepository.findAll())
                .thenReturn(List.of(existente));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo(planilhaBase())));

        assertTrue(erro.getMessage().contains(
                "Professor já cadastrado ou repetido"));

        verificarNenhumaPersistencia();
    }

    @Test
    void deveRejeitarDisciplinaRepetidaNaTurma()
            throws Exception {

        XSSFWorkbook workbook = planilhaBase();

        workbook.getSheet("Turmas")
                .getRow(1).getCell(4).setCellValue("D1,D1");

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.importar(arquivo(workbook)));

        assertTrue(erro.getMessage().contains(
                "Disciplina repetida"));

        verificarNenhumaPersistencia();
    }
}
