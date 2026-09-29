package com.academic.service;

import com.academic.model.CursoPeriodo;
import com.academic.repository.CursoPeriodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CursoPeriodoService {

    @Autowired
    private CursoPeriodoRepository repository;

    public List<CursoPeriodo> listarTodos() {
        return repository.findAll();
    }

    public Optional<CursoPeriodo> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public CursoPeriodo guardar(CursoPeriodo cursoPeriodo) {
        return repository.save(cursoPeriodo);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
