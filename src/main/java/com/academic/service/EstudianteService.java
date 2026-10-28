package com.academic.service;

import com.academic.model.Estudiante;
import com.academic.repository.EstudianteRepository;
import com.academic.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Por apellido y nombre, para los selectores. */
    public List<Estudiante> listarOrdenados() {
        return estudianteRepository.findAllByOrderByApellidoAscNombreAsc();
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

    /** Registra un estudiante nuevo; el id y el usuario enviados por el formulario se ignoran. */
    @Transactional
    public Estudiante registrar(Estudiante datos) {
        datos.setId(null);
        datos.setUsuario(null);
        return guardar(datos);
    }

    /**
     * Actualiza los datos editables de un estudiante existente.
     * Se valida antes de tocar el registro guardado, para no dejarlo modificado si hay un error.
     */
    @Transactional
    public Estudiante actualizar(Estudiante datos) {
        Validaciones.exigir(datos.getId() != null, "El estudiante no existe.");
        normalizar(datos);
        validar(datos);
        Estudiante estudiante = estudianteRepository.findById(datos.getId())
                .orElseThrow(() -> new IllegalArgumentException("El estudiante no existe."));
        estudiante.setNombre(datos.getNombre());
        estudiante.setApellido(datos.getApellido());
        estudiante.setDni(datos.getDni());
        estudiante.setEmail(datos.getEmail());
        estudiante.setTelefono(datos.getTelefono());
        estudiante.setFechaInscripcion(datos.getFechaInscripcion());
        enlazarUsuario(estudiante);
        return estudianteRepository.save(estudiante);
    }

    public Estudiante guardar(Estudiante estudiante) {
        normalizar(estudiante);
        validar(estudiante);
        enlazarUsuario(estudiante);
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
        return esDeOtro(estudianteRepository.findByDni(dni), excludeId);
    }

    /** Verifica si ya existe otro estudiante con ese email (sin distinguir mayúsculas). */
    public boolean existeEmail(String email, Long excludeId) {
        if (Validaciones.vacio(email)) return false;
        return esDeOtro(estudianteRepository.findFirstByEmailIgnoreCase(email.trim()), excludeId);
    }

    /** Verifica si ya existe otro estudiante con ese teléfono. Un teléfono vacío no se compara. */
    public boolean existeTelefono(String telefono, Long excludeId) {
        if (Validaciones.vacio(telefono)) return false;
        return esDeOtro(estudianteRepository.findFirstByTelefono(telefono.trim()), excludeId);
    }

    private void normalizar(Estudiante e) {
        e.setNombre(Validaciones.limpiar(e.getNombre()));
        e.setApellido(Validaciones.limpiar(e.getApellido()));
        e.setDni(Validaciones.limpiar(e.getDni()));
        e.setEmail(Validaciones.limpiar(e.getEmail()));
        e.setTelefono(Validaciones.limpiar(e.getTelefono()));
    }

    private void validar(Estudiante e) {
        Validaciones.persona(e.getNombre(), e.getApellido(), e.getDni(), e.getEmail(), e.getTelefono());
        Validaciones.exigir(!existeDni(e.getDni(), e.getId()), "dni",
                "Este DNI ya está registrado en otro estudiante.");
        Validaciones.exigir(!existeEmail(e.getEmail(), e.getId()), "email",
                "Este email ya está registrado en otro estudiante.");
        Validaciones.exigir(!existeTelefono(e.getTelefono(), e.getId()), "telefono",
                "Este teléfono ya está registrado en otro estudiante.");
    }

    /** Se enlaza con el usuario ESTUDIANTE que tenga el mismo email, si aún está libre. */
    private void enlazarUsuario(Estudiante estudiante) {
        if (estudiante.getUsuario() != null) return;
        usuarioRepository.findByEmail(estudiante.getEmail())
                .filter(u -> "ESTUDIANTE".equals(u.getRol()))
                .filter(u -> estudianteRepository.findByUsuarioId(u.getId()).isEmpty())
                .ifPresent(estudiante::setUsuario);
    }

    private static boolean esDeOtro(Optional<Estudiante> existente, Long excludeId) {
        return existente.isPresent() && (excludeId == null || !existente.get().getId().equals(excludeId));
    }
}
