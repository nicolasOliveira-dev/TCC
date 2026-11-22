package com.example.EduGrid.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.EduGrid.model.GradeHoraria;
import com.example.EduGrid.model.Horario;
import com.example.EduGrid.model.Professor;
import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.GradeHorariaRepository;
import com.example.EduGrid.repository.HorarioRepository;
import com.example.EduGrid.solver.domain.HorarioPlanejamento;
import com.example.EduGrid.solver.domain.RegraHorarioEscolar;

@Service
public class GradeEdicaoService {

    private final GradeHorariaRepository gradeRepository;
    private final HorarioRepository horarioRepository;

    public GradeEdicaoService(GradeHorariaRepository gradeRepository, HorarioRepository horarioRepository) {
        this.gradeRepository = gradeRepository;
        this.horarioRepository = horarioRepository;
    }

    public record Movimento(Integer turmaId, int diaOrigem, int periodoOrigem,
            int diaDestino, int periodoDestino, boolean confirmarSoft) {}

    public record Resultado(boolean valido, boolean salvo, String mensagem,
            List<String> restricoesHard, List<String> restricoesSoft,
            int quantidadePeriodos) {}

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarParaTela(GradeHoraria grade) {
        return horarioRepository.findByGradeHoraria(grade).stream().map(h -> Map.<String, Object>of(
                "id", h.getId(), "turmaId", h.getTurma().getId(), "dia", h.getDiaSemana(),
                "periodo", h.getPeriodoDia(), "turno", h.getTurno(),
                "disciplina", h.getDisciplina().getNome(),
                "professor", h.getProfessor() == null ? "" : h.getProfessor().getNome()
        )).toList();
    }

    @Transactional
    public Resultado mover(Integer gradeId, Movimento movimento, boolean salvar) {
        GradeHoraria grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Grade não encontrada."));
        List<Horario> horarios = horarioRepository.findByGradeHorariaForUpdate(grade);
        if (horarios.isEmpty()) {
            return erro("Esta grade não possui horários editáveis vinculados.");
        }
        Tamanho dimensoes = new Tamanho(grade.getTamanhoGrade().getQtdeDiasSemana(),
                grade.getTamanhoGrade().getQtdePeriodosDia());
        List<Horario> bloco = bloco(horarios, movimento.turmaId(), movimento.diaOrigem(), movimento.periodoOrigem());
        if (bloco.isEmpty()) {
            return erro("Selecione uma aula existente.");
        }
        bloco.sort(Comparator.comparingInt(Horario::getPeriodoDia));
        int turno = bloco.get(0).getTurno();
        int inicioOrigem = bloco.get(0).getPeriodoDia();
        if (bloco.stream().anyMatch(h -> h.getTurno() != turno
                || h.getDiaSemana() != movimento.diaOrigem()
                || h.getPeriodoDia() != inicioOrigem + bloco.indexOf(h))) {
            return erro("O bloco selecionado possui horários inconsistentes.");
        }
        if (!dentro(movimento.diaDestino(), movimento.periodoDestino(), bloco.size(), dimensoes)) {
            return erro("O bloco não cabe nesse dia/período.");
        }
        List<Horario> destino = new ArrayList<>(bloco(horarios, movimento.turmaId(), movimento.diaDestino(), movimento.periodoDestino()));
        destino.sort(Comparator.comparingInt(Horario::getPeriodoDia));
        if (!destino.isEmpty() && bloco.containsAll(destino)) {
            return new Resultado(true, false, "O destino já pertence ao bloco selecionado.", List.of(), List.of(), bloco.size());
        }
        boolean trocar = !destino.isEmpty() && !destino.stream().allMatch(bloco::contains);
        int inicioDestinoBloco = destino.isEmpty() ? movimento.periodoDestino()
                : destino.stream().mapToInt(Horario::getPeriodoDia).min().orElse(movimento.periodoDestino());
        if (destino.stream().anyMatch(h -> h.getDiaSemana() != movimento.diaDestino()
                || h.getPeriodoDia() != inicioDestinoBloco + destino.indexOf(h))) {
            return erro("O bloco de destino possui horários inconsistentes.");
        }

        if (!dentro(movimento.diaDestino(), inicioDestinoBloco, bloco.size(), dimensoes)
                || (trocar && !dentro(movimento.diaOrigem(), inicioOrigem, destino.size(), dimensoes))) {
            return erro("Os blocos n\u00e3o cabem nas posi\u00e7\u00f5es de destino.");
        }

        List<Posicao> origens = bloco.stream().map(Posicao::de).toList();
        List<Posicao> destinos = destino.stream().map(Posicao::de).toList();
        bloco.forEach(h -> colocar(h, movimento.diaDestino(), inicioDestinoBloco, inicioOrigem));
        if (trocar) {
            destino.forEach(h -> colocar(h, movimento.diaOrigem(), inicioOrigem, inicioDestinoBloco));
        }

        List<String> hard = validar(horarios, grade, dimensoes);
        List<String> soft = avisosSoft(horarios);
        if (salvar) {
            bloco.forEach(h -> h.setManualmenteEditado(true));
            if (trocar) {
                destino.forEach(h -> h.setManualmenteEditado(true));
            }
            horarioRepository.saveAll(horarios);
            grade.setRelatorioEdicao(descreverResultadoEdicao(hard, soft));
            gradeRepository.save(grade);
            return new Resultado(true, true, mensagemResultadoEdicao(hard, soft), hard, soft, bloco.size());
        }
        if (!hard.isEmpty()) {
            restaurar(bloco, origens);
            restaurar(destino, destinos);
            return new Resultado(true, false, "Pr\u00e9via calculada. Viola\u00e7\u00f5es n\u00e3o bloqueiam a edi\u00e7\u00e3o.", hard, soft, bloco.size());
        }
        if (salvar && !soft.isEmpty() && !movimento.confirmarSoft()) {
            restaurar(bloco, origens);
            restaurar(destino, destinos);
            return new Resultado(true, false, "Confirme as restrições flexíveis para salvar.", hard, soft, bloco.size());
        }
        if (salvar) {
            bloco.forEach(h -> h.setManualmenteEditado(true));
            if (trocar) {
                destino.forEach(h -> h.setManualmenteEditado(true));
            }
            horarioRepository.saveAll(horarios);
        } else {
            restaurar(bloco, origens);
            restaurar(destino, destinos);
        }
        return new Resultado(true, salvar, salvar ? "Alteração salva." : "Prévia válida.", hard, soft, bloco.size());
    }

    private String mensagemResultadoEdicao(List<String> hard, List<String> soft) {
        if (hard.isEmpty() && soft.isEmpty()) {
            return "Altera\u00e7\u00e3o salva sem viola\u00e7\u00f5es detectadas.";
        }
        return "Altera\u00e7\u00e3o salva com " + hard.size() + " viola\u00e7\u00e3o(\u00f5es) obrigat\u00f3ria(s) e "
                + soft.size() + " aviso(s) flex\u00edvel(is).";
    }

    private String descreverResultadoEdicao(List<String> hard, List<String> soft) {
        StringBuilder relatorio = new StringBuilder("Resultado da edi\u00e7\u00e3o manual:\n");
        if (hard.isEmpty() && soft.isEmpty()) {
            return relatorio.append("Nenhuma viola\u00e7\u00e3o de restri\u00e7\u00e3o foi detectada.").toString();
        }
        if (!hard.isEmpty()) {
            relatorio.append("Restri\u00e7\u00f5es obrigat\u00f3rias infringidas:\n");
            hard.forEach(item -> relatorio.append("- ").append(item).append('\n'));
        }
        if (!soft.isEmpty()) {
            relatorio.append("Avisos de restri\u00e7\u00f5es flex\u00edveis:\n");
            soft.forEach(item -> relatorio.append("- ").append(item).append('\n'));
        }
        return relatorio.toString().trim();
    }

    private List<Horario> bloco(List<Horario> todos, Integer turmaId, int dia, int periodo) {
        Horario selecionado = todos.stream().filter(h -> Objects.equals(h.getTurma().getId(), turmaId)
                && h.getDiaSemana() == dia && h.getPeriodoDia() == periodo).findFirst().orElse(null);
        if (selecionado == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(todos.stream().filter(h -> Objects.equals(h.getTurma().getId(), turmaId)
                && Objects.equals(id(h.getDisciplina()), id(selecionado.getDisciplina()))
                && Objects.equals(id(h.getProfessor()), id(selecionado.getProfessor()))
                && h.getDiaSemana() == dia && h.getTurno() == selecionado.getTurno())
                .filter(h -> conectado(todos, selecionado, h, turmaId, dia))
                .toList());
    }

    private boolean conectado(List<Horario> todos, Horario selecionado, Horario atual, Integer turmaId, int dia) {
        int menor = Math.min(selecionado.getPeriodoDia(), atual.getPeriodoDia());
        int maior = Math.max(selecionado.getPeriodoDia(), atual.getPeriodoDia());
        for (int p = menor; p <= maior; p++) {
            final int periodo = p;
            if (todos.stream().noneMatch(h -> Objects.equals(h.getTurma().getId(), turmaId)
                    && h.getDiaSemana() == dia && h.getTurno() == selecionado.getTurno()
                    && h.getPeriodoDia() == periodo
                    && Objects.equals(id(h.getDisciplina()), id(selecionado.getDisciplina()))
                    && Objects.equals(id(h.getProfessor()), id(selecionado.getProfessor())))) {
                return false;
            }
        }
        return true;
    }

    private List<String> validar(List<Horario> horarios, GradeHoraria grade, Tamanho dimensoes) {
        List<String> erros = new ArrayList<>();
        for (Horario h : horarios) {
            if (!dentro(h.getDiaSemana(), h.getPeriodoDia(), 1, dimensoes)) {
                erros.add("Horário fora dos limites da grade.");
            }
            if (!turnoCompativel(h)) {
                erros.add("Turno incompatível ou não reconhecido para a turma " + (h.getTurma() == null ? "(sem turma)" : h.getTurma().getNome()) + ".");
            }
            if (RegraHorarioEscolar.ocupaPeriodoProibidoNaQuarta(new HorarioPlanejamento(h.getDiaSemana(), h.getPeriodoDia(), h.getTurno()), 1, dimensoes.periodos)) {
                erros.add("A quarta-feira não permite aulas nos dois últimos períodos da manhã ou tarde.");
            }
            if (!disponivel(h)) {
                erros.add("Professor indisponível: " + nome(h.getProfessor()) + ", " + h.getDisciplina().getNome() + ".");
            }
        }
        for (Horario h : horarios) {
            if (h.getProfessor() != null && (h.getProfessor().getDisciplinasPossiveis() == null
                    || h.getProfessor().getDisciplinasPossiveis().stream().noneMatch(d -> Objects.equals(id(d), id(h.getDisciplina()))))) {
                erros.add("Professor nǜo habilitado para " + h.getDisciplina().getNome() + ": " + h.getProfessor().getNome() + ".");
            }
        }
        Map<Integer, List<Horario>> porProfessor = horarios.stream().filter(h -> h.getProfessor() != null)
                .collect(java.util.stream.Collectors.groupingBy(h -> h.getProfessor().getId()));
        for (List<Horario> aulasProfessor : porProfessor.values()) {
            long diasOcupados = aulasProfessor.stream().map(Horario::getDiaSemana).distinct().count();
            if (diasOcupados >= dimensoes.dias) {
                erros.add("Professor sem dia de planejamento livre: " + aulasProfessor.get(0).getProfessor().getNome() + ".");
            }
            for (int i = 0; i < aulasProfessor.size(); i++) {
                for (int j = i + 1; j < aulasProfessor.size(); j++) {
                    Horario a = aulasProfessor.get(i), b = aulasProfessor.get(j);
                    if ((a.getTurno() == 2 && b.getTurno() == 0 && b.getDiaSemana() == a.getDiaSemana() + 1)
                            || (b.getTurno() == 2 && a.getTurno() == 0 && a.getDiaSemana() == b.getDiaSemana() + 1)) {
                        erros.add("Descanso insuficiente entre noite e manhã: " + a.getProfessor().getNome() + ".");
                    }
                }
            }
            var porDiaTurno = aulasProfessor.stream().collect(java.util.stream.Collectors.groupingBy(
                    h -> h.getDiaSemana() + ":" + h.getTurno()));
            porDiaTurno.values().forEach(aulas -> {
                List<Integer> periodos = aulas.stream().map(Horario::getPeriodoDia).sorted().toList();
                int inicio = 0, fim = -1;
                for (int periodo : periodos) {
                    if (periodo > fim + 1) {
                        inicio = periodo;
                    }
                    fim = Math.max(fim, periodo);
                    if (fim - inicio + 1 > aulas.get(0).getProfessor().getJornadaMaxima()) {
                        erros.add("Jornada máxima excedida: " + aulas.get(0).getProfessor().getNome() + ".");
                        break;
                    }
                }
            });
        }
        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {
                Horario a = horarios.get(i), b = horarios.get(j);
                if (a.getDiaSemana() == b.getDiaSemana() && a.getTurno() == b.getTurno() && a.getPeriodoDia() == b.getPeriodoDia()) {
                    if (Objects.equals(id(a.getTurma()), id(b.getTurma()))) {
                        erros.add("Conflito de turma: " + a.getTurma().getNome() + ".");
                    }
                    if (a.getProfessor() != null && Objects.equals(id(a.getProfessor()), id(b.getProfessor()))) {
                        erros.add("Conflito de professor: " + a.getProfessor().getNome() + ".");
                    }
                }
            }
        }
        return erros.stream().distinct().toList();
    }

    private boolean turnoCompativel(Horario horario) {
        if (horario.getTurma() == null || horario.getTurma().getCurso() == null) {
            return false;
        }
        try {
            return horario.getTurno() == HorarioPlanejamento.indiceDoTurno(
                    horario.getTurma().getCurso().getTurno());
        } catch (IllegalArgumentException turnoDesconhecido) {
            return false;
        }
    }

    private boolean disponivel(Horario h) {
        Professor p = h.getProfessor();
        if (p == null) {
            return true;
        }
        Boolean[][][] d = p.getDisponibilidadeHoraria();
        int turno = h.getTurno(), dia = h.getDiaSemana(), periodo = h.getPeriodoDia();
        return d != null && turno >= 0 && turno < d.length && d[turno] != null
                && periodo >= 0 && periodo < d[turno].length && d[turno][periodo] != null
                && dia >= 0 && dia < d[turno][periodo].length && Boolean.TRUE.equals(d[turno][periodo][dia]);
    }

    private List<String> avisosSoft(List<Horario> horarios) {
        List<String> avisos = new ArrayList<>();
        for (Horario h : horarios) {
            if (h.getProfessor() != null && h.getProfessor().getIntencaoDiaPlanejamento() != null
                    && h.getProfessor().getIntencaoDiaPlanejamento().stream().anyMatch(d -> d != null && d.equalsIgnoreCase(diaNome(h.getDiaSemana())))) {
                avisos.add("Aula no dia de planejamento preferido de " + h.getProfessor().getNome() + ".");
            }
        }
        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {
                Horario a = horarios.get(i), b = horarios.get(j);
                if (a.getProfessor() != null && Objects.equals(id(a.getProfessor()), id(b.getProfessor()))
                        && a.getDiaSemana() == b.getDiaSemana() && a.getTurno() == b.getTurno()
                        && !Objects.equals(id(a.getDisciplina()), id(b.getDisciplina()))
                        && Math.abs(a.getPeriodoDia() - b.getPeriodoDia()) == 1) {
                    avisos.add("Aulas de disciplinas diferentes ficam consecutivas para " + a.getProfessor().getNome() + ".");
                }
            }
        }
        return avisos.stream().distinct().toList();
    }

    private String diaNome(int dia) {
        return List.of("Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo").get(Math.max(0, Math.min(dia, 6)));
    }

    private boolean dentro(int d, int p, int duracao, Tamanho t) {
        return d >= 0 && d < t.dias && p >= 0 && duracao > 0 && p + duracao <= t.periodos;
    }

    private Integer id(Object entidade) {
        if (entidade instanceof Turma t) {
            return t.getId();
        
        }if (entidade instanceof Professor p) {
            return p.getId();
        
        }if (entidade instanceof com.example.EduGrid.model.Disciplina d) {
            return d.getId();
        
        }return null;
    }

    private String nome(Professor p) {
        return p == null ? "(sem professor)" : p.getNome();
    }

    private void colocar(Horario h, int dia, int periodoInicial, int inicioAnterior) {
        h.setDiaSemana(dia);
        h.setPeriodoDia(periodoInicial + h.getPeriodoDia() - inicioAnterior);
    }

    private void restaurar(List<Horario> horarios, List<Posicao> posicoes) {
        for (int i = 0; i < horarios.size(); i++) {
            horarios.get(i).setDiaSemana(posicoes.get(i).dia);
            horarios.get(i).setPeriodoDia(posicoes.get(i).periodo);
        }
    }

    private Resultado erro(String m) {
        return new Resultado(false, false, m, List.of(m), List.of(), 0);
    }

    private record Tamanho(int dias, int periodos) {}

    private record Posicao(int dia, int periodo) {

        static Posicao de(Horario h) {
            return new Posicao(h.getDiaSemana(), h.getPeriodoDia());
        }
    }
}
