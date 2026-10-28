package com.academic.service;

import com.academic.model.Docente;
import com.academic.repository.DocenteRepository;
import com.academic.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Registra un docente nuevo; el id y el usuario enviados por el formulario se ignoran. */
    @Transactional
    public Docente registrar(Docente datos) {
        datos.setId(null);
        datos.setUsuario(null);
        return guardar(datos);
    }

    /**
     * Actualiza los datos editables de un docente existente.
     * Se valida antes de tocar el registro guardado, para no dejarlo modificado si hay un error.
     */
    @Transactional
    public Docente actualizar(Docente datos) {
        Validaciones.exigir(datos.getId() != null, "El docente no existe.");
        normalizar(datos);
        validar(datos);
        Docente docente = docenteRepository.findById(datos.getId())
                .orElseThrow(() -> new IllegalArgumentException("El docente no existe."));
        docente.setNombre(datos.getNombre());
        docente.setApellido(datos.getApellido());
        docente.setDni(datos.getDni());
        docente.setTelefono(datos.getTelefono());
        docente.setEspecialidad(datos.getEspecialidad());
        docente.setEmail(datos.getEmail());
        enlazarUsuario(docente);
        return docenteRepository.save(docente);
    }

    public Docente guardar(Docente docente) {
        normalizar(docente);
        validar(docente);
        enlazarUsuario(docente);
        return docenteRepository.save(docente);
    }

    public void eliminar(Long id) {
        docenteRepository.deleteById(id);
    }

    public long contarTotal() {
        return docenteRepository.count();
    }

    public boolean existeDni(String dni, Long excludeId) {
        return esDeOtro(docenteRepository.findByDni(dni), excludeId);
    }

    /** Verifica si ya existe otro docente con ese email (sin distinguir mayúsculas). */
    public boolean existeEmail(String email, Long excludeId) {
        if (Validaciones.vacio(email)) return false;
        return esDeOtro(docenteRepository.findFirstByEmailIgnoreCase(email.trim()), excludeId);
    }

    public boolean existeTelefono(String telefono, Long excludeId) {
        if (Validaciones.vacio(telefono)) return false;
        return esDeOtro(docenteRepository.findByTelefono(telefono.trim()), excludeId);
    }

    private void normalizar(Docente d) {
        d.setNombre(Validaciones.limpiar(d.getNombre()));
        d.setApellido(Validaciones.limpiar(d.getApellido()));
        d.setDni(Validaciones.limpiar(d.getDni()));
        d.setEmail(Validaciones.limpiar(d.getEmail()));
        d.setTelefono(Validaciones.limpiar(d.getTelefono()));
        d.setEspecialidad(Validaciones.limpiar(d.getEspecialidad()));
    }

    private void validar(Docente d) {
        Validaciones.persona(d.getNombre(), d.getApellido(), d.getDni(), d.getEmail(), d.getTelefono());
        Validaciones.exigir(d.getEspecialidad() == null || d.getEspecialidad().length() <= 150, "especialidad",
                "La especialidad no puede superar los 150 caracteres.");
        Validaciones.exigir(!existeDni(d.getDni(), d.getId()), "dni",
                "Este DNI ya está registrado en otro docente.");
        Validaciones.exigir(!existeEmail(d.getEmail(), d.getId()), "email",
                "Este email ya está registrado en otro docente.");
        Validaciones.exigir(!existeTelefono(d.getTelefono(), d.getId()), "telefono",
                "Este teléfono ya está registrado en otro docente.");
    }

    /** Se enlaza con el usuario DOCENTE que tenga el mismo email, si aún está libre. */
    private void enlazarUsuario(Docente docente) {
        if (docente.getUsuario() != null) return;
        usuarioRepository.findByEmail(docente.getEmail())
                .filter(u -> "DOCENTE".equals(u.getRol()))
                .filter(u -> docenteRepository.findByUsuarioId(u.getId()).isEmpty())
                .ifPresent(docente::setUsuario);
    }

    private static boolean esDeOtro(Optional<Docente> existente, Long excludeId) {
        return existente.isPresent() && (excludeId == null || !existente.get().getId().equals(excludeId));
    }
}
