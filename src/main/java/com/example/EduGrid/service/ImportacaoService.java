package com.example.EduGrid.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.model.converter.BooleanMatrixConverter;
import com.example.EduGrid.model.converter.StringListConverter;
import com.example.EduGrid.repository.CursoRepository;
import com.example.EduGrid.repository.DisciplinaRepository;
import com.example.EduGrid.repository.ProfessorRepository;
import com.example.EduGrid.repository.TurmaRepository;

@Service
public class ImportacaoService {

    private final CursoRepository cursoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final TurmaRepository turmaRepository;
    private final ProfessorRepository professorRepository;

    public ImportacaoService(
            CursoRepository cursoRepository,
            DisciplinaRepository disciplinaRepository,
            TurmaRepository turmaRepository,
            ProfessorRepository professorRepository) {

        this.cursoRepository = cursoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.turmaRepository = turmaRepository;
        this.professorRepository = professorRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultadoImportacao importar(MultipartFile arquivo)
            throws IOException {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selecione uma planilha para importar.");
        }

        String nomeArquivo = arquivo.getOriginalFilename();
        if (nomeArquivo == null
                || !nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException(
                    "O arquivo deve estar no formato .xlsx.");
        }

        try (Workbook workbook = new XSSFWorkbook(
                arquivo.getInputStream())) {

            Map<String, List<Map<String, String>>> dados
                    = new HashMap<>();

            for (String aba : List.of(
                    "Cursos", "Disciplinas", "Turmas", "Professores")) {
                dados.put(aba, lerAba(workbook, aba));
            }

            validar(dados);

            return persistir(dados);
        }
    }

    private List<Map<String, String>> lerAba(
            Workbook workbook, String nome) {

        Sheet sheet = workbook.getSheet(nome);

        if (sheet == null) {
            throw new IllegalArgumentException(
                    "A aba '" + nome + "' não foi encontrada.");
        }

        DataFormatter formatter = new DataFormatter(
                Locale.forLanguageTag("pt-BR"));

        Row cabecalho = sheet.getRow(0);

        if (cabecalho == null) {
            throw new IllegalArgumentException(
                    "A aba '" + nome + "' não possui cabeçalho.");
        }

        List<String> colunas = new ArrayList<>();
        Set<String> repetidas = new HashSet<>();

        for (int c = 0; c < cabecalho.getLastCellNum(); c++) {
            String coluna = formatter.formatCellValue(
                    cabecalho.getCell(c)).trim();

            if (coluna.isBlank() || !repetidas.add(coluna)) {
                throw new IllegalArgumentException(
                        "Cabeçalho vazio ou duplicado na aba " + nome);
            }

            colunas.add(coluna);
        }

        List<Map<String, String>> registros = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row linha = sheet.getRow(i);
            if (linha == null) {
                continue;
            }

            Map<String, String> registro = new HashMap<>();
            boolean preenchida = false;

            for (int c = 0; c < colunas.size(); c++) {
                String valor = formatter.formatCellValue(
                        linha.getCell(c)).trim();

                registro.put(colunas.get(c), valor);
                preenchida |= !valor.isBlank();
            }

            if (preenchida) {
                registro.put("_linha", String.valueOf(i + 1));
                registros.add(registro);
            }
        }

        return registros;
    }

    private void validar(
            Map<String, List<Map<String, String>>> dados) {

        Map<String, List<String>> colunas = Map.of(
                "Cursos", List.of(
                        "codigo", "nomeCurso", "tipoCurso", "turno"),
                "Disciplinas", List.of(
                        "codigo", "nome", "areaAtuacao",
                        "cargaHoraria", "curso", "tipoDisciplina",
                        "minPeriodosJuntos", "maxPeriodosJuntos"),
                "Turmas", List.of(
                        "codigo", "nome", "curso", "ano", "disciplinas"),
                "Professores", List.of(
                        "codigo", "nome", "areaAtuacao",
                        "cargaHoraria", "disciplinasPossiveis")
        );

        for (String aba : colunas.keySet()) {
            for (Map<String, String> registro : dados.get(aba)) {
                for (String coluna : colunas.get(aba)) {
                    if (valor(registro, coluna).isBlank()) {
                        throw erro(aba, registro,
                                "Campo obrigatório: " + coluna);
                    }
                }
            }

            Set<String> codigos = new HashSet<>();

            for (Map<String, String> registro : dados.get(aba)) {
                if (!codigos.add(valor(registro, "codigo"))) {
                    throw erro(aba, registro,
                            "Código duplicado na planilha.");
                }
            }
        }

        Set<String> cursos = codigos(dados.get("Cursos"));
        Set<String> disciplinas
                = codigos(dados.get("Disciplinas"));

        for (Map<String, String> d : dados.get("Disciplinas")) {
            if (!cursos.contains(valor(d, "curso"))) {
                throw erro("Disciplinas", d,
                        "Curso inexistente: " + valor(d, "curso"));
            }

            double carga = decimal(d, "cargaHoraria", "Disciplinas");
            int minimo = inteiro(d, "minPeriodosJuntos", "Disciplinas");
            int maximo = inteiro(d, "maxPeriodosJuntos", "Disciplinas");

            if (carga <= 0 || minimo <= 0 || maximo < minimo) {
                throw erro("Disciplinas", d,
                        "Carga horária ou limites de blocos inválidos.");
            }

            String tipo = normalizar(valor(d, "tipoDisciplina"));

            if (!Set.of("tecnica", "comum", "eletiva",
                    "nivelamento").contains(tipo)) {
                throw erro("Disciplinas", d,
                        "Tipo de disciplina inválido.");
            }
        }

        for (Map<String, String> t : dados.get("Turmas")) {
            if (!cursos.contains(valor(t, "curso"))) {
                throw erro("Turmas", t, "Curso inexistente.");
            }

            if (inteiro(t, "ano", "Turmas") <= 0) {
                throw erro("Turmas", t, "Ano inválido.");
            }

            validarReferencias(t, "disciplinas",
                    disciplinas, "Turmas");
        }

        for (Map<String, String> p : dados.get("Professores")) {
            if (inteiro(p, "cargaHoraria", "Professores") <= 0) {
                throw erro("Professores", p,
                        "Carga horária inválida.");
            }

            validarReferencias(p, "disciplinasPossiveis",
                    disciplinas, "Professores");
        }

        validarDuplicidadesExistentes(dados);
    }

    private void validarDuplicidadesExistentes(
            Map<String, List<Map<String, String>>> dados) {

        Set<String> cursos = cursoRepository.findAll().stream()
                .map(c -> normalizar(c.getNomeCurso()))
                .collect(Collectors.toSet());

        Set<String> disciplinas = disciplinaRepository.findAll().stream()
                .map(d -> d.getCurso() == null
                        ? normalizar(d.getNome())
                        : d.getCurso().getId() + ":" + normalizar(d.getNome()))
                .collect(Collectors.toSet());

        Set<String> turmas = turmaRepository.findAll().stream()
                .map(t -> normalizar(t.getNome()))
                .collect(Collectors.toSet());

        Set<String> professores = professorRepository.findAll().stream()
                .map(p -> normalizar(p.getNome()))
                .collect(Collectors.toSet());

        Map<String, String> nomesCursos = new HashMap<>();
        for (Map<String, String> c : dados.get("Cursos")) {
            String nome = normalizar(valor(c, "nomeCurso"));
            if (!cursos.add(nome)) {
                throw erro("Cursos", c, "Curso já cadastrado ou repetido.");
            }
            nomesCursos.put(valor(c, "codigo"), nome);
        }

        Set<String> disciplinasArquivo = new HashSet<>();
        for (Map<String, String> d : dados.get("Disciplinas")) {
            String chave = valor(d, "curso") + ":"
                    + normalizar(valor(d, "nome"));

            if (!disciplinasArquivo.add(chave)) {
                throw erro("Disciplinas", d,
                        "Disciplina repetida para o mesmo curso.");
            }
        }

        // Como esta versão exige cursos novos na planilha,
        // os cursos importados ainda não possuem IDs no banco.
        for (Map<String, String> t : dados.get("Turmas")) {
            if (!turmas.add(normalizar(valor(t, "nome")))) {
                throw erro("Turmas", t,
                        "Turma já cadastrada ou repetida.");
            }
        }

        for (Map<String, String> p : dados.get("Professores")) {
            if (!professores.add(normalizar(valor(p, "nome")))) {
                throw erro("Professores", p,
                        "Professor já cadastrado ou repetido.");
            }
        }
    }

    private ResultadoImportacao persistir(
            Map<String, List<Map<String, String>>> dados) {

        Map<String, Curso> cursos = new HashMap<>();
        Map<String, Disciplina> disciplinas = new HashMap<>();

        for (Map<String, String> r : dados.get("Cursos")) {
            Curso curso = new Curso(
                    valor(r, "nomeCurso"),
                    valor(r, "tipoCurso"),
                    valor(r, "turno")
            );

            cursos.put(valor(r, "codigo"),
                    cursoRepository.save(curso));
        }

        for (Map<String, String> r : dados.get("Disciplinas")) {
            Disciplina disciplina = new Disciplina();

            disciplina.setNome(valor(r, "nome"));
            disciplina.setAreaAtuacao(valor(r, "areaAtuacao"));
            disciplina.setCurso(cursos.get(valor(r, "curso")));
            disciplina.setTipoDisciplina(
                    valor(r, "tipoDisciplina"));

            disciplina.setMinPeriodosJuntos(
                    inteiro(r, "minPeriodosJuntos", "Disciplinas"));
            disciplina.setMaxPeriodosJuntos(
                    inteiro(r, "maxPeriodosJuntos", "Disciplinas"));

            disciplina.setCargaHoraria(
                    decimal(r, "cargaHoraria", "Disciplinas"));

            disciplinas.put(valor(r, "codigo"),
                    disciplinaRepository.save(disciplina));
        }

        for (Map<String, String> r : dados.get("Turmas")) {
            Turma turma = new Turma();

            turma.setNome(valor(r, "nome"));
            turma.setAno(inteiro(r, "ano", "Turmas"));
            turma.setCurso(cursos.get(valor(r, "curso")));
            turma.setDisciplinas(
                    resolverDisciplinas(
                            valor(r, "disciplinas"), disciplinas));

            turmaRepository.save(turma);
        }

        for (Map<String, String> r : dados.get("Professores")) {
            Professor professor = new Professor();

            professor.setNome(valor(r, "nome"));
            professor.setAreaAtuacao(valor(r, "areaAtuacao"));
            professor.setCargaHoraria(
                    inteiro(r, "cargaHoraria", "Professores"));

            professor.setDisciplinasPossiveis(
                    resolverDisciplinas(
                            valor(r, "disciplinasPossiveis"),
                            disciplinas));

            String disponibilidadeHoraria = valor(r, "disponibilidadeHoraria");
            professor.setDisponibilidadeHoraria(disponibilidadeHoraria.isBlank()
                    ? null
                    : new BooleanMatrixConverter().convertToEntityAttribute(disponibilidadeHoraria));
            String intencaoDiaPlanejamento = valor(r, "intencaoDiaPlanejamento");
            professor.setIntencaoDiaPlanejamento(
                    new StringListConverter().convertToEntityAttribute(intencaoDiaPlanejamento));

            professorRepository.save(professor);
        }

        return new ResultadoImportacao(
                dados.get("Cursos").size(),
                dados.get("Disciplinas").size(),
                dados.get("Turmas").size(),
                dados.get("Professores").size()
        );
    }

    private List<Disciplina> resolverDisciplinas(
            String codigos, Map<String, Disciplina> disciplinas) {

        List<Disciplina> resultado = new ArrayList<>();

        for (String codigo : separar(codigos)) {
            resultado.add(disciplinas.get(codigo));
        }

        return resultado;
    }

    private void validarReferencias(
            Map<String, String> registro,
            String coluna,
            Set<String> existentes,
            String aba) {

        Set<String> encontradas = new HashSet<>();

        for (String codigo : separar(valor(registro, coluna))) {
            if (!existentes.contains(codigo)) {
                throw erro(aba, registro,
                        "Disciplina inexistente: " + codigo);
            }

            if (!encontradas.add(codigo)) {
                throw erro(aba, registro,
                        "Disciplina repetida: " + codigo);
            }
        }
    }

    private Set<String> codigos(
            List<Map<String, String>> registros) {

        return registros.stream()
                .map(r -> valor(r, "codigo"))
                .collect(Collectors.toSet());
    }

    private List<String> separar(String texto) {
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String valor(Map<String, String> r, String campo) {
        return r.getOrDefault(campo, "").trim();
    }

    private int inteiro(
            Map<String, String> r, String campo, String aba) {
        try {
            return Integer.parseInt(valor(r, campo));
        } catch (NumberFormatException e) {
            throw erro(aba, r, "Número inteiro inválido: " + campo);
        }
    }

    private double decimal(
            Map<String, String> r, String campo, String aba) {
        try {
            return Double.parseDouble(
                    valor(r, campo).replace(",", "."));
        } catch (NumberFormatException e) {
            throw erro(aba, r, "Número decimal inválido: " + campo);
        }
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(),
                Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private IllegalArgumentException erro(
            String aba, Map<String, String> registro,
            String mensagem) {

        return new IllegalArgumentException(
                "Aba " + aba
                + ", linha " + registro.get("_linha")
                + ": " + mensagem);
    }

    public record ResultadoImportacao(
            int cursos,
            int disciplinas,
            int turmas,
            int professores) {

    }
}
