package com.academic.service;

import com.academic.model.Estudiante;
import com.academic.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstudianteService {

    @Autowired
    private EstudianteRepository estudianteRepository;

    public List<Estudiante> listarTodos() {
        return estudianteRepository.findAll();
    }

    public List<Estudiante> buscar(String termino) {
        return estudianteRepository
                .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(termino, termino);
    }

    public Optional<Estudiante> buscarPorId(Long id) {
        return estudianteRepository.findById(id);
    }

    public Estudiante guardar(Estudiante estudiante) {
        return estudianteRepository.save(estudiante);
    }

    public void eliminar(Long id) {
        estudianteRepository.deleteById(id);
    }

    public long contarTotal() {
        return estudianteRepository.count();
    }

    /**
     * Verifica si ya existe un estudiante con ese DNI,
     * excluyendo el registro con excludeId (útil para edición).
     * Si excludeId es null, verifica sin excluir ninguno (útil para creación).
     */
    public boolean existeDni(String dni, Long excludeId) {
        Optional<Estudiante> existente = estudianteRepository.findByDni(dni);
        if (existente.isEmpty()) return false;
        if (excludeId == null) return true;
        return !existente.get().getId().equals(excludeId);
    }

    /**
     * Verifica si ya existe un estudiante con ese email,
     * excluyendo el registro con excludeId.
     */
    public boolean existeEmail(String email, Long excludeId) {
        Optional<Estudiante> existente = estudianteRepository.findByEmail(email);
        if (existente.isEmpty()) return false;
        if (excludeId == null) return true;
        return !existente.get().getId().equals(excludeId);
    }
}
