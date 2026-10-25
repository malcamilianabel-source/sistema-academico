package com.academic.service;

import com.academic.model.Estudiante;
import com.academic.repository.EstudianteRepository;
import com.academic.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EstudianteService {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    public Optional<Estudiante> buscarPorUsuario(Long usuarioId) {
        return estudianteRepository.findByUsuarioId(usuarioId);
    }

    public Estudiante guardar(Estudiante estudiante) {
        Validaciones.persona(estudiante.getNombre(), estudiante.getApellido(), estudiante.getDni(),
                estudiante.getEmail(), estudiante.getTelefono());

        // Se enlaza con el usuario ESTUDIANTE que tenga el mismo email, si aún está libre
        if (estudiante.getUsuario() == null) {
            usuarioRepository.findByEmail(estudiante.getEmail())
                    .filter(u -> "ESTUDIANTE".equals(u.getRol()))
                    .filter(u -> estudianteRepository.findByUsuarioId(u.getId()).isEmpty())
                    .ifPresent(estudiante::setUsuario);
        }
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
