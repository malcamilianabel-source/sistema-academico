package com.academic.service;

import com.academic.model.Curso;
import com.academic.model.CursoPeriodo;
import com.academic.model.PeriodoAcademico;
import com.academic.model.Sesion;
import com.academic.model.Usuario;
import com.academic.repository.CursoRepository;
import com.academic.repository.DocenteRepository;
import com.academic.repository.EstudianteRepository;
import com.academic.repository.SesionRepository;
import com.academic.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Sesiones (horas y fechas), cursos (código único) y usuarios (email único, contraseña). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SesionYCursoServiceTest {

    @Mock SesionRepository sesionRepository;
    @Mock CursoRepository cursoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock EstudianteRepository estudianteRepository;

    private SesionService sesionService() {
        SesionService s = new SesionService();
        ReflectionTestUtils.setField(s, "repository", sesionRepository);
        when(sesionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        return s;
    }

    private Sesion sesion(String fecha, String inicio, String fin) {
        PeriodoAcademico p = new PeriodoAcademico();
        p.setNombre("2026-II");
        p.setFechaInicio(LocalDate.parse("2026-08-17"));
        p.setFechaFin(LocalDate.parse("2026-12-12"));
        CursoPeriodo cp = new CursoPeriodo();
        cp.setPeriodo(p);
        Sesion s = new Sesion();
        s.setCursoPeriodo(cp);
        s.setFechaSesion(LocalDate.parse(fecha));
        s.setHoraInicio(inicio == null ? null : LocalTime.parse(inicio));
        s.setHoraFin(fin == null ? null : LocalTime.parse(fin));
        s.setAula("A-201");
        s.setTipo("PRESENCIAL");
        return s;
    }

    @Test
    void sesionConHoraDeInicioPeroSinHoraDeFin() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> sesionService().guardar(sesion("2026-09-01", "08:00", null)));
        assertEquals("horaFin", e.getCampo());
    }

    @Test
    void sesionConHoraDeFinAnteriorAlInicio() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> sesionService().guardar(sesion("2026-09-01", "10:00", "08:00")));
        assertEquals("horaFin", e.getCampo());
    }

    @Test
    void sesionFueraDelPeriodo() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> sesionService().guardar(sesion("2026-12-20", "08:00", "10:00")));
        assertEquals("fechaSesion", e.getCampo());
        assertTrue(e.getMessage().contains("17/08/2026 al 12/12/2026"));
    }

    @Test
    void sesionValidaSinHorasODentroDelPeriodo() {
        SesionService service = sesionService();
        assertDoesNotThrow(() -> service.guardar(sesion("2026-08-17", null, null)));
        assertDoesNotThrow(() -> service.guardar(sesion("2026-12-12", "08:00", "10:00")));
    }

    @Test
    void codigoDeCursoRepetidoSinImportarMayusculas() {
        CursoService service = new CursoService();
        ReflectionTestUtils.setField(service, "cursoRepository", cursoRepository);
        Curso existente = new Curso();
        existente.setId(1L);
        when(cursoRepository.findFirstByCodigoIgnoreCase("isw-301")).thenReturn(Optional.of(existente));
        Curso nuevo = new Curso();
        nuevo.setNombre("Programación");
        nuevo.setCodigo("isw-301");

        ValidacionException e = assertThrows(ValidacionException.class, () -> service.guardar(nuevo));

        assertEquals("codigo", e.getCampo());
        verify(cursoRepository, never()).save(any());
    }

    private UsuarioService usuarioService() {
        UsuarioService s = new UsuarioService();
        ReflectionTestUtils.setField(s, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(s, "docenteRepository", docenteRepository);
        ReflectionTestUtils.setField(s, "estudianteRepository", estudianteRepository);
        when(usuarioRepository.findFirstByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        return s;
    }

    private Usuario usuario(Long id, String email, String password) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Carlos");
        u.setApellido("Ramírez");
        u.setEmail(email);
        u.setPassword(password);
        u.setRol("DOCENTE");
        return u;
    }

    @Test
    void emailDeUsuarioRepetidoSeRechazaEnSuCampo() {
        UsuarioService service = usuarioService();
        when(usuarioRepository.findFirstByEmailIgnoreCase("cramirez@academico.com"))
                .thenReturn(Optional.of(usuario(7L, "cramirez@academico.com", "x")));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.registrar(usuario(null, "cramirez@academico.com", "clave123")));

        assertEquals("email", e.getCampo());
    }

    @Test
    void contrasenaCortaSeRechazaEnSuCampoYNoSeGuardaEnTextoPlano() {
        UsuarioService service = usuarioService();
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.registrar(usuario(null, "nuevo@academico.com", "123")));
        assertEquals("password", e.getCampo());

        Usuario guardado = service.registrar(usuario(null, "nuevo@academico.com", "clave123"));
        assertTrue(guardado.getPassword().startsWith("$2"));
    }

    @Test
    void editarSinContrasenaConservaLaActual() {
        UsuarioService service = usuarioService();
        Usuario actual = usuario(7L, "cramirez@academico.com", "$2a$10$hashanterior");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(actual));

        Usuario datos = usuario(7L, "cramirez@academico.com", null);
        datos.setNombre("Carlos Alberto");
        service.actualizar(datos);

        assertEquals("Carlos Alberto", actual.getNombre());
        assertEquals("$2a$10$hashanterior", actual.getPassword());
    }
}
