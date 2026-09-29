package com.academic.service;

import com.academic.model.PeriodoAcademico;
import com.academic.repository.PeriodoAcademicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PeriodoAcademicoService {

    @Autowired
    private PeriodoAcademicoRepository repository;

    public List<PeriodoAcademico> listarTodos() {
        return repository.findAll();
    }

    public Optional<PeriodoAcademico> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public PeriodoAcademico guardar(PeriodoAcademico periodo) {
        return repository.save(periodo);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public long contarTotal() {
        return repository.count();
    }
}
