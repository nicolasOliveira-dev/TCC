package com.example.EduGrid.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.Disciplina;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.repository.TurmaRepository;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final HorarioRepository horarioRepository;
    private final BlocoDisciplinaService blocoDisciplinaService;
    private final GradeHorariaService gradeHorariaService;

    @Autowired
    public TurmaService(
            TurmaRepository turmaRepository,
            HorarioRepository horarioRepository,
            BlocoDisciplinaService blocoDisciplinaService,
            GradeHorariaService gradeHorariaService) {
        this.turmaRepository = turmaRepository;
        this.horarioRepository = horarioRepository;
        this.blocoDisciplinaService = blocoDisciplinaService;
        this.gradeHorariaService = gradeHorariaService;
    }

    /** Mantido para instanciação simples em código legado e testes. */
    public TurmaService(TurmaRepository turmaRepository, HorarioRepository horarioRepository,
            BlocoDisciplinaService blocoDisciplinaService) {
        this(turmaRepository, horarioRepository, blocoDisciplinaService, null);
    }

    public Turma salvarTurma(Turma turma) {
        return turmaRepository.save(turma);
    }

    public Turma BuscarTurmaPorId(Integer id) {
        return turmaRepository.findById(id).orElse(null);
    }

    public void excluirTurma(Integer id) {
        turmaRepository.deleteById(id);
    }

    public Turma atualizarTurma(Integer id, Turma turmaAtualizada) {
        Turma turmaExistente = turmaRepository.findById(id).orElse(null);
        if (turmaExistente != null) {
            turmaExistente.setNome(turmaAtualizada.getNome());
            turmaExistente.setCurso(turmaAtualizada.getCurso());
            turmaExistente.setDisciplinas(turmaAtualizada.getDisciplinas());
            turmaExistente.setAno(turmaAtualizada.getAno());
            return turmaRepository.save(turmaExistente);
        }
        return null;
    }

    public List<Turma> listarTurmas() {
        return turmaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Turma> listarTurmasParaOtimizacao() {
        return turmaRepository.findAllParaOtimizacao();
    }

    @Transactional(readOnly = true)
    public List<Horario> listarHorariosDaGradeAtual(Integer turmaId) {
        Turma turma = turmaRepository.findById(turmaId).orElse(null);
        if (turma == null) {
            return List.of();
        }
        var grade = gradeHorariaService == null ? null : gradeHorariaService.buscarGradeMaisRecente();
        return grade == null
                ? horarioRepository.findByTurmaAndGradeHorariaIsNull(turma)
                : horarioRepository.findByTurmaAndGradeHoraria(turma, grade);
    }

    @Transactional
    public void alocarGradeHoraria(Turma turma, TamanhoGrade tamanhoGrade, ControleAlocacaoGlobal controleGlobal) {

        horarioRepository.deleteByTurma(turma);
        turma.setTamanhoGrade(tamanhoGrade);
        turma.setGradeHorariaMatriz(tamanhoGrade);
        List<Disciplina> disciplinas = turma.getDisciplinas();
        if (disciplinas == null) {
            return;
        }

        int turno = turma.getCurso() == null ? 0 : classificarTurno(turma.getCurso().getTurno());
        if (turno < 0) {
            throw new IllegalStateException("Turno não reconhecido para a turma: " + turma.getNome());
        }

        List<Horario> horariosAlocados = new ArrayList<>();
        for (Disciplina disciplina : disciplinas) {
            if (disciplina == null) {
                continue;
            }

            disciplina.setTamanhoGrade(tamanhoGrade);
            int periodosNecessarios = disciplina.getNumPeriodos();
            String chaveDisciplina = disciplina.getNome().trim().toLowerCase();

            List<Integer> blocosDefinidos;
            try {
                blocosDefinidos = blocoDisciplinaService.calcularBlocos(disciplina, tamanhoGrade);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                System.out.println(exception.getMessage());
                continue;
            }
            if (blocosDefinidos.isEmpty()) {
                continue;
            }

            int restantes = periodosNecessarios;
            int blocosRestantes = blocosDefinidos.size();

            boolean[] diaJaUtilizado = new boolean[tamanhoGrade.getQtdeDiasSemana()];

            while (restantes > 0) {
                boolean alocado = false;
                int tamanhoBlocoAlocado = 0;
                int diaAlocado = -1;
                List<Integer> tamanhosCandidatos = blocoDisciplinaService.calcularTamanhosCandidatos(
                        disciplina,
                        tamanhoGrade,
                        restantes,
                        blocosRestantes
                );

                for (int tamanhoBloco : tamanhosCandidatos) {
                    if (alocado) {
                        break;
                    }
                    for (int dia = 0; dia < tamanhoGrade.getQtdeDiasSemana() && !alocado; dia++) {
                        if (diaJaUtilizado[dia]) {
                            continue;
                        }

                        for (int inicio = 0;
                                inicio <= tamanhoGrade.getQtdePeriodosDia() - tamanhoBloco && !alocado;
                                inicio++) {
                            boolean espacoDisponivel = true;

                            for (int i = 0; i < tamanhoBloco; i++) {

                                int periodoAtual = inicio + i;

                                boolean horarioOcupadoNaTurma = turma.getGradeHorariaMatriz()[dia][periodoAtual] != 0;

                                boolean disciplinaOcupadaEmOutraTurma = !controleGlobal.estaDisponivel(
                                        chaveDisciplina, dia, periodoAtual, turno);

                                if (horarioOcupadoNaTurma || disciplinaOcupadaEmOutraTurma) {
                                    espacoDisponivel = false;
                                    break;
                                }
                            }

                            if (espacoDisponivel) {
                                for (int i = 0; i < tamanhoBloco; i++) {

                                    int periodoAtual = inicio + i;

                                    turma.getGradeHorariaMatriz()[dia][periodoAtual] = disciplina.getId();
                                    controleGlobal.registrar(chaveDisciplina, dia, periodoAtual, turno);
                                    horariosAlocados.add(new Horario(
                                            null,
                                            turno,
                                            periodoAtual,
                                            dia,
                                            turma,
                                            disciplina,
                                            null));
                                }
                                alocado = true;
                                tamanhoBlocoAlocado = tamanhoBloco;
                                diaAlocado = dia;
                            }
                        }
                    }
                }

                if (!alocado) {
                    System.out.println("Não foi possível encontrar espaço para um bloco válido da disciplina: "
                            + disciplina.getNome());
                    break;
                }

                restantes -= tamanhoBlocoAlocado;
                blocosRestantes--;
                diaJaUtilizado[diaAlocado] = true;
            }
        }

        horarioRepository.saveAll(horariosAlocados);
    }

    private int classificarTurno(String turno) {
        if (turno == null) {
            return 0;
        }

        String normalizado = Normalizer.normalize(turno, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (normalizado.contains("matutino") || normalizado.contains("manha")) {
            return 0;
        }
        if (normalizado.contains("vespertino") || normalizado.contains("tarde")) {
            return 1;
        }
        if (normalizado.contains("noturno") || normalizado.contains("noite")) {
            return 2;
        }
        return -1;
    }
}
