package com.example.EduGrid.service;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.TamanhoGrade;
import com.example.EduGrid.repository.TamanhoGradeRepository;

@Service
public class TamanhoGradeService {
    private final TamanhoGradeRepository repository;

    public TamanhoGradeService(TamanhoGradeRepository repository) {
        this.repository = repository;
    }

    public TamanhoGrade salvar(TamanhoGrade tamanhoGrade) {
        return repository.save(tamanhoGrade);
    }

    public TamanhoGrade buscarPorId(Integer id) {
        return repository.findById(id).orElse(null);
    }

    public TamanhoGrade buscarConfiguracao() {
        return repository.findFirstByOrderByIdDesc().orElse(null);
    }

    public TamanhoGrade atualizar(Integer id, TamanhoGrade atualizado) {
        TamanhoGrade existente = repository.findById(id).orElse(null);

        if (existente == null || atualizado == null) {
            return null;
        }

        existente.setQtdeTurnos(atualizado.getQtdeTurnos());
        existente.setQtdePeriodosDia(atualizado.getQtdePeriodosDia());
        existente.setQtdeDiasSemana(atualizado.getQtdeDiasSemana());
        existente.setTamanhoPeriodo(atualizado.getTamanhoPeriodo());

        return repository.save(existente);
    }

    public void excluir(Integer id) {
        repository.deleteById(id);
    }
}