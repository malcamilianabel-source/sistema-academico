package com.academic.service;

import com.academic.model.Docente;
import com.academic.repository.DocenteRepository;
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
class DocenteServiceTest {

    @Mock DocenteRepository repository;
    @Mock UsuarioRepository usuarios;
    @InjectMocks DocenteService service;

    @BeforeEach
    void sinDuplicados() {
        when(repository.findByDni(any())).thenReturn(Optional.empty());
        when(repository.findFirstByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(repository.findByTelefono(any())).thenReturn(Optional.empty());
        when(usuarios.findByEmail(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Docente docente(Long id, String telefono) {
        Docente d = new Docente();
        d.setId(id);
        d.setNombre("Carlos");
        d.setApellido("Ramírez");
        d.setDni("40123456");
        d.setEmail("cramirez@academico.com");
        d.setTelefono(telefono);
        return d;
    }

    @Test
    void telefonoRepetidoMuestraElMensajeAcordadoEnSuCampo() {
        when(repository.findByTelefono("987654321")).thenReturn(Optional.of(docente(3L, "987654321")));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.registrar(docente(null, "987654321")));

        assertEquals("telefono", e.getCampo());
        assertEquals("Este teléfono ya está registrado en otro docente.", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void emailRepetidoSinImportarMayusculas() {
        when(repository.findFirstByEmailIgnoreCase("CRamirez@academico.com"))
                .thenReturn(Optional.of(docente(3L, null)));
        Docente nuevo = docente(null, null);
        nuevo.setEmail("CRamirez@academico.com");

        ValidacionException e = assertThrows(ValidacionException.class, () -> service.registrar(nuevo));

        assertEquals("email", e.getCampo());
    }

    @Test
    void sinTelefonoNoSeComparaContraOtros() {
        Docente guardado = service.registrar(docente(null, "  "));
        assertNull(guardado.getTelefono());
        verify(repository, never()).findByTelefono(any());
    }

    @Test
    void especialidadMuyLargaSeRechazaEnSuCampo() {
        Docente d = docente(null, null);
        d.setEspecialidad("x".repeat(151));
        ValidacionException e = assertThrows(ValidacionException.class, () -> service.registrar(d));
        assertEquals("especialidad", e.getCampo());
    }
}
