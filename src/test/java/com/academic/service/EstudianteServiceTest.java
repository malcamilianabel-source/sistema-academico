package com.academic.service;

import com.academic.model.Estudiante;
import com.academic.model.Usuario;
import com.academic.repository.EstudianteRepository;
import com.academic.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstudianteServiceTest {

    @Mock EstudianteRepository repository;
    @Mock UsuarioRepository usuarios;
    @InjectMocks EstudianteService service;

    @BeforeEach
    void sinDuplicados() {
        when(repository.findByDni(any())).thenReturn(Optional.empty());
        when(repository.findFirstByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(repository.findFirstByTelefono(any())).thenReturn(Optional.empty());
        when(usuarios.findByEmail(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Estudiante estudiante(Long id, String dni, String telefono) {
        Estudiante e = new Estudiante();
        e.setId(id);
        e.setNombre("María");
        e.setApellido("Quispe");
        e.setDni(dni);
        e.setEmail("mquispe@academico.com");
        e.setTelefono(telefono);
        return e;
    }

    @Test
    void rechazaTelefonoDeOtroEstudianteEnElCampoTelefono() {
        when(repository.findFirstByTelefono("987654321")).thenReturn(Optional.of(estudiante(5L, "11111111", "987654321")));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.registrar(estudiante(null, "72845619", "987654321")));

        assertEquals("telefono", e.getCampo());
        assertEquals("Este teléfono ya está registrado en otro estudiante.", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaDniRepetidoEnElCampoDni() {
        when(repository.findByDni("72845619")).thenReturn(Optional.of(estudiante(5L, "72845619", null)));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.registrar(estudiante(null, "72845619", null)));

        assertEquals("dni", e.getCampo());
    }

    @Test
    void alEditarPuedeConservarSuPropioTelefono() {
        Estudiante guardado = estudiante(5L, "72845619", "987654321");
        when(repository.findFirstByTelefono("987654321")).thenReturn(Optional.of(guardado));
        when(repository.findByDni("72845619")).thenReturn(Optional.of(guardado));
        when(repository.findById(5L)).thenReturn(Optional.of(guardado));

        Estudiante datos = estudiante(5L, "72845619", "987654321");
        datos.setNombre("María Fernanda");
        service.actualizar(datos);

        assertEquals("María Fernanda", guardado.getNombre());
        verify(repository).save(guardado);
    }

    @Test
    void siLaEdicionFallaElRegistroGuardadoNoCambia() {
        Estudiante guardado = estudiante(5L, "72845619", "900000000");
        when(repository.findById(5L)).thenReturn(Optional.of(guardado));
        when(repository.findFirstByTelefono("987654321")).thenReturn(Optional.of(estudiante(9L, "11111111", "987654321")));

        Estudiante datos = estudiante(5L, "72845619", "987654321");
        assertThrows(ValidacionException.class, () -> service.actualizar(datos));

        assertEquals("900000000", guardado.getTelefono());
        verify(repository, never()).save(any());
    }

    @Test
    void registrarIgnoraIdYUsuarioEnviadosPorElFormulario() {
        Estudiante datos = estudiante(99L, "72845619", null);
        datos.setUsuario(new Usuario());

        Estudiante guardado = service.registrar(datos);

        assertNull(guardado.getId());
        assertNull(guardado.getUsuario());
    }

    @Test
    void quitaEspaciosAntesDeValidar() {
        Estudiante datos = estudiante(null, " 72845619 ", " 987654321 ");

        Estudiante guardado = service.registrar(datos);

        assertEquals("72845619", guardado.getDni());
        assertEquals("987654321", guardado.getTelefono());
    }

    @Test
    void telefonoConLetrasSeRechazaEnSuCampo() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.registrar(estudiante(null, "72845619", "98765432a")));
        assertEquals("telefono", e.getCampo());
    }
}
