package com.example.EduGrid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Turma;
import com.example.EduGrid.repository.TurmaRepository;

@Service
public class TurmaService {
    private final TurmaRepository turmaRepository;

    public TurmaService(TurmaRepository turmaRepository) {
        this.turmaRepository = turmaRepository;
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
            turmaExistente.setDisciplina(turmaAtualizada.getDisciplina());
            turmaExistente.setCurso(turmaAtualizada.getCurso());
            turmaExistente.setHistoricoProfessor(turmaAtualizada.getHistoricoProfessor());
            return turmaRepository.save(turmaExistente);
        }
        return null;
    }

    public List<Turma> listarTurmas() {
        return turmaRepository.findAll();
    }
}
