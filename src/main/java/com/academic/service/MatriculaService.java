package com.academic.service;

import com.academic.model.Matricula;
import com.academic.repository.MatriculaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MatriculaService {

    @Autowired
    private MatriculaRepository repository;

    public List<Matricula> listarTodos() {
        return repository.findAll();
    }

    public Optional<Matricula> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Matricula guardar(Matricula matricula) {
        return repository.save(matricula);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public List<Matricula> buscarPorEstudiante(Long estudianteId) {
        return repository.findByEstudianteId(estudianteId);
    }

    public long contarTotal() {
        return repository.count();
    }
}
