package com.academic.service;

import com.academic.model.Docente;
import com.academic.repository.DocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DocenteService {

    @Autowired
    private DocenteRepository docenteRepository;

    public List<Docente> listarTodos() {
        return docenteRepository.findAll();
    }

    public List<Docente> buscar(String termino) {
        return docenteRepository
                .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(termino, termino);
    }

    public Optional<Docente> buscarPorId(Long id) {
        return docenteRepository.findById(id);
    }

    public Docente guardar(Docente docente) {
        return docenteRepository.save(docente);
    }

    public void eliminar(Long id) {
        docenteRepository.deleteById(id);
    }

    public long contarTotal() {
        return docenteRepository.count();
    }

    public boolean existeDni(String dni, Long excludeId) {
        Optional<Docente> existente = docenteRepository.findByDni(dni);
        if (existente.isEmpty()) return false;
        if (excludeId == null) return true;
        return !existente.get().getId().equals(excludeId);
    }

    public boolean existeEmail(String email, Long excludeId) {
        Optional<Docente> existente = docenteRepository.findByEmail(email);
        if (existente.isEmpty()) return false;
        if (excludeId == null) return true;
        return !existente.get().getId().equals(excludeId);
    }

    public boolean existeTelefono(String telefono, Long excludeId) {
        if (telefono == null || telefono.isBlank()) return false;

        Optional<Docente> existente = docenteRepository.findByTelefono(telefono);
        if (existente.isEmpty()) return false;
        if (excludeId == null) return true;
        return !existente.get().getId().equals(excludeId);
    }
}
