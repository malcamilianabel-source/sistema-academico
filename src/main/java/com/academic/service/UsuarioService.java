package com.academic.service;

import com.academic.model.Usuario;
import com.academic.repository.DocenteRepository;
import com.academic.repository.EstudianteRepository;
import com.academic.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<String> ROLES = Set.of("ADMIN", "DOCENTE", "ESTUDIANTE");
    private static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String HASH_BCRYPT = "^\\$2[aby]\\$\\d{2}\\$.{53}$";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public Optional<Usuario> login(String email, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) return Optional.empty();

        Usuario usuario = usuarioOpt.get();
        String guardada = usuario.getPassword();
        if (guardada.matches(HASH_BCRYPT)) {
            if (!encoder.matches(password, guardada)) return Optional.empty();
        } else {
            // Contraseña antigua en texto plano: se comprueba y se reemplaza por su hash
            if (!guardada.equals(password)) return Optional.empty();
            usuario.setPassword(encoder.encode(password));
            usuarioRepository.save(usuario);
        }
        sincronizarVinculo(usuario);
        return Optional.of(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /** Valida la contraseña escrita por el usuario y devuelve su hash BCrypt. */
    public String codificarPassword(String password) {
        Validaciones.exigir(password != null && password.length() >= 4, "password",
                "La contraseña debe tener al menos 4 caracteres.");
        return encoder.encode(password);
    }

    /** Registra un usuario nuevo con la contraseña escrita en el formulario (se guarda su hash). */
    @Transactional
    public Usuario registrar(Usuario datos) {
        datos.setId(null);
        validarDatos(datos);
        datos.setPassword(codificarPassword(datos.getPassword()));
        return guardar(datos);
    }

    /**
     * Actualiza nombre, apellido, email, rol y, si se escribió, la contraseña.
     * Se valida antes de tocar el registro guardado, para no dejarlo modificado si hay un error.
     */
    @Transactional
    public Usuario actualizar(Usuario datos) {
        Validaciones.exigir(datos.getId() != null, "El usuario no existe.");
        validarDatos(datos);
        String nuevoHash = datos.getPassword() == null ? null : codificarPassword(datos.getPassword());
        Usuario usuario = usuarioRepository.findById(datos.getId())
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
        usuario.setNombre(datos.getNombre());
        usuario.setApellido(datos.getApellido());
        usuario.setEmail(datos.getEmail());
        usuario.setRol(datos.getRol());
        if (nuevoHash != null) usuario.setPassword(nuevoHash);
        return guardar(usuario);
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        validarDatos(usuario);
        Usuario guardado = usuarioRepository.save(usuario);
        sincronizarVinculo(guardado);
        return guardado;
    }

    private void validarDatos(Usuario usuario) {
        usuario.setNombre(Validaciones.limpiar(usuario.getNombre()));
        usuario.setApellido(Validaciones.limpiar(usuario.getApellido()));
        usuario.setEmail(Validaciones.limpiar(usuario.getEmail()));
        Validaciones.nombreYApellido(usuario.getNombre(), usuario.getApellido());
        Validaciones.exigir(Validaciones.coincide(usuario.getEmail(), EMAIL) && usuario.getEmail().length() <= 150,
                "email", "Ingresa un email válido.");
        Validaciones.exigir(usuarioRepository.findFirstByEmailIgnoreCase(usuario.getEmail())
                        .filter(u -> !u.getId().equals(usuario.getId())).isEmpty(),
                "email", "Este email ya está registrado en otro usuario.");
        Validaciones.exigir(usuario.getRol() != null && ROLES.contains(usuario.getRol()),
                "rol", "El rol debe ser ADMIN, DOCENTE o ESTUDIANTE.");
    }

    @Transactional
    public void eliminar(Long id) {
        docenteRepository.findByUsuarioId(id).ifPresent(d -> {
            d.setUsuario(null);
            docenteRepository.save(d);
        });
        estudianteRepository.findByUsuarioId(id).ifPresent(e -> {
            e.setUsuario(null);
            estudianteRepository.save(e);
        });
        usuarioRepository.deleteById(id);
    }

    public long contarTotal() {
        return usuarioRepository.count();
    }

    /**
     * Enlaza el usuario con el docente o estudiante que tiene su mismo email, según su rol,
     * y lo desenlaza si el rol ya no corresponde.
     */
    private void sincronizarVinculo(Usuario usuario) {
        var docente = docenteRepository.findByUsuarioId(usuario.getId());
        if (!"DOCENTE".equals(usuario.getRol())) {
            docente.ifPresent(d -> {
                d.setUsuario(null);
                docenteRepository.save(d);
            });
        } else if (docente.isEmpty()) {
            docenteRepository.findByEmail(usuario.getEmail())
                    .filter(d -> d.getUsuario() == null)
                    .ifPresent(d -> {
                        d.setUsuario(usuario);
                        docenteRepository.save(d);
                    });
        }

        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId());
        if (!"ESTUDIANTE".equals(usuario.getRol())) {
            estudiante.ifPresent(e -> {
                e.setUsuario(null);
                estudianteRepository.save(e);
            });
        } else if (estudiante.isEmpty()) {
            estudianteRepository.findByEmail(usuario.getEmail())
                    .filter(e -> e.getUsuario() == null)
                    .ifPresent(e -> {
                        e.setUsuario(usuario);
                        estudianteRepository.save(e);
                    });
        }
    }
}
