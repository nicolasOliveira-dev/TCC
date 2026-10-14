package com.example.EduGrid.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.EduGrid.model.Curso;
import com.example.EduGrid.repository.CursoRepository;

@Service
public class CursoService {
    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public List<Curso> listarCursos() {
        return cursoRepository.findAll();
    }

    public Curso buscarCursoPorId(Integer id) {
        return cursoRepository.findById(id).orElse(null);
    }

    public Curso salvarCurso(Curso curso) {
        return cursoRepository.save(curso);
    }

    public Curso atualizarCurso(Integer id, Curso cursoAtualizado) {
        return cursoRepository.findById(id).map(curso -> {
            curso.setNomeCurso(cursoAtualizado.getNomeCurso());
            curso.setTipoCurso(cursoAtualizado.getTipoCurso());
            curso.setTurno(cursoAtualizado.getTurno());
            return cursoRepository.save(curso);
        }).orElse(null);
    }

    public void excluirCurso(Integer id) {
        cursoRepository.deleteById(id);
    }
}
