package com.academic.service;

import com.academic.model.Evaluacion;
import com.academic.repository.EvaluacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EvaluacionService {

    @Autowired
    private EvaluacionRepository repository;

    public List<Evaluacion> listarTodos() {
        return repository.findAll();
    }

    public Optional<Evaluacion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Evaluacion guardar(Evaluacion evaluacion) {
        if (evaluacion.getNota() != null && (evaluacion.getNota() < 0 || evaluacion.getNota() > 20)) {
            throw new IllegalArgumentException("La nota debe estar entre 0 y 20");
        }
        return repository.save(evaluacion);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public List<Evaluacion> buscarPorMatricula(Long matriculaId) {
        return repository.findByMatriculaId(matriculaId);
    }
}
