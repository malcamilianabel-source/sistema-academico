package com.academic.service;

import com.academic.model.Docente;
import com.academic.repository.DocenteRepository;
import com.academic.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DocenteService {

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    public Optional<Docente> buscarPorUsuario(Long usuarioId) {
        return docenteRepository.findByUsuarioId(usuarioId);
    }

    public Docente guardar(Docente docente) {
        Validaciones.persona(docente.getNombre(), docente.getApellido(), docente.getDni(),
                docente.getEmail(), docente.getTelefono());
        String especialidad = docente.getEspecialidad();
        Validaciones.exigir(especialidad == null || especialidad.length() <= 150,
                "La especialidad no puede superar los 150 caracteres.");

        // Se enlaza con el usuario DOCENTE que tenga el mismo email, si aún está libre
        if (docente.getUsuario() == null) {
            usuarioRepository.findByEmail(docente.getEmail())
                    .filter(u -> "DOCENTE".equals(u.getRol()))
                    .filter(u -> docenteRepository.findByUsuarioId(u.getId()).isEmpty())
                    .ifPresent(docente::setUsuario);
        }
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
