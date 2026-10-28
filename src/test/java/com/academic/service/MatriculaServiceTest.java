package com.academic.service;

import com.academic.dto.MatriculaForm;
import com.academic.model.*;
import com.academic.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatriculaServiceTest {
    @Mock MatriculaRepository repository;
    @Mock CursoPeriodoRepository cursos;
    @Mock EstudianteRepository estudiantes;
    @Mock PeriodoAcademicoRepository periodos;
    @InjectMocks MatriculaService service;
    Estudiante estudiante;
    PeriodoAcademico abierto;

    @BeforeEach void preparar() {
        estudiante = new Estudiante();
        estudiante.setId(7L);
        abierto = periodo(1L, "2026-II");
        abierto.setMatriculaAbierta(true);
        when(periodos.findFirstByMatriculaAbiertaTrue()).thenReturn(Optional.of(abierto));
        when(estudiantes.buscarPorUsuarioConBloqueo(1L)).thenReturn(Optional.of(estudiante));
    }

    private PeriodoAcademico periodo(Long id, String nombre) {
        PeriodoAcademico p = new PeriodoAcademico(); p.setId(id); p.setNombre(nombre);
        return p;
    }

    /** Sección del curso {cursoId} en el periodo dado, con 40 vacantes. */
    private CursoPeriodo seccion(Long id, Long cursoId, PeriodoAcademico p) {
        Curso c = new Curso(); c.setId(cursoId); c.setNombre("Curso " + cursoId);
        CursoPeriodo cp = new CursoPeriodo(); cp.setId(id); cp.setCurso(c); cp.setPeriodo(p); cp.setSeccion("A");
        when(cursos.buscarConBloqueo(id)).thenReturn(Optional.of(cp));
        return cp;
    }

    @Test void registraVariosCursosParaElUsuario() {
        seccion(1L, 1L, abierto);
        seccion(2L, 2L, abierto);
        service.matricularCursos(1L, List.of(1L, 2L));
        verify(repository, times(2)).save(argThat(m -> m.getEstudiante() == estudiante && "ACTIVO".equals(m.getEstado())));
    }

    @Test void rechazaTodaLaSeleccionSiUnCursoEsDeOtroPeriodo() {
        seccion(1L, 1L, abierto);
        seccion(2L, 2L, periodo(2L, "2026-I"));
        assertThrows(IllegalArgumentException.class, () -> service.matricularCursos(1L, List.of(1L, 2L)));
        verify(repository, never()).save(any());
    }

    private Matricula previa(CursoPeriodo cp, String estado) {
        Matricula m = new Matricula(); m.setId(50L); m.setEstudiante(estudiante); m.setCursoPeriodo(cp); m.setEstado(estado);
        when(repository.findFirstByEstudianteIdAndCursoPeriodoCursoIdAndCursoPeriodoPeriodoId(
                7L, cp.getCurso().getId(), cp.getPeriodo().getId())).thenReturn(Optional.of(m));
        return m;
    }

    @Test void rechazaCursoYaMatriculado() {
        previa(seccion(1L, 1L, abierto), "ACTIVO");
        assertThrows(IllegalArgumentException.class, () -> service.matricularCursos(1L, List.of(1L)));
        verify(repository, never()).save(any());
    }

    @Test void rechazaDosSeccionesDelMismoCurso() {
        seccion(1L, 1L, abierto);
        seccion(2L, 1L, abierto).setSeccion("B");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.matricularCursos(1L, List.of(1L, 2L)));
        assertEquals("No puedes seleccionar dos secciones del mismo curso.", e.getMessage());
    }

    @Test void rechazaSeccionSinVacantes() {
        CursoPeriodo cp = seccion(1L, 1L, abierto);
        cp.setVacantes(30);
        when(repository.countByCursoPeriodoIdAndEstado(1L, "ACTIVO")).thenReturn(30L);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.matricularCursos(1L, List.of(1L)));
        assertTrue(e.getMessage().contains("ya no tiene vacantes"));
        verify(repository, never()).save(any());
    }

    @Test void conLaMatriculaCerradaNoSeRegistraNada() {
        when(periodos.findFirstByMatriculaAbiertaTrue()).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.matricularCursos(1L, List.of(1L)));
        assertTrue(service.cursosDisponibles().isEmpty());
        assertNull(service.getPeriodoMatricula());
    }

    @Test void bajaConservaElRegistro() {
        Matricula m = new Matricula(); m.setEstado("ACTIVO"); m.setCursoPeriodo(seccion(1L, 1L, abierto));
        when(repository.findById(1L)).thenReturn(Optional.of(m));
        service.darDeBaja(1L);
        assertEquals("RETIRADO", m.getEstado());
        verify(repository).save(m);
        verify(repository, never()).deleteById(any());
    }

    @Test void noSeRetiraEnUnPeriodoCerrado() {
        PeriodoAcademico cerrado = periodo(3L, "2025-II");
        cerrado.setEstado(PeriodoAcademico.ESTADO_CERRADO);
        Matricula m = new Matricula(); m.setEstado("ACTIVO"); m.setCursoPeriodo(seccion(1L, 1L, cerrado));
        when(repository.findById(1L)).thenReturn(Optional.of(m));
        assertThrows(IllegalArgumentException.class, () -> service.darDeBaja(1L));
        assertEquals("ACTIVO", m.getEstado());
    }

    // ── Matrícula registrada por el administrador ──

    private MatriculaForm form(Long seccionId) {
        when(estudiantes.buscarConBloqueo(7L)).thenReturn(Optional.of(estudiante));
        MatriculaForm f = new MatriculaForm(); f.setEstudianteId(7L); f.setCursoPeriodoId(seccionId);
        return f;
    }

    @Test void adminMatriculaAunqueLaMatriculaDeEstudiantesEsteCerrada() {
        PeriodoAcademico planificado = periodo(2L, "2027-I");
        seccion(5L, 1L, planificado);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        Matricula m = service.registrarPorAdmin(form(5L));
        assertEquals("ACTIVO", m.getEstado());
        assertSame(estudiante, m.getEstudiante());
    }

    @Test void adminNoMatriculaEnPeriodoCerrado() {
        PeriodoAcademico cerrado = periodo(3L, "2025-II");
        cerrado.setEstado(PeriodoAcademico.ESTADO_CERRADO);
        seccion(5L, 1L, cerrado);
        ValidacionException e = assertThrows(ValidacionException.class, () -> service.registrarPorAdmin(form(5L)));
        assertEquals("cursoPeriodoId", e.getCampo());
        verify(repository, never()).save(any());
    }

    @Test void adminNoMatriculaEnOtraSeccionDelMismoCurso() {
        CursoPeriodo a = seccion(1L, 1L, abierto);
        CursoPeriodo b = seccion(2L, 1L, abierto); b.setSeccion("B");
        previa(a, "ACTIVO");
        ValidacionException e = assertThrows(ValidacionException.class, () -> service.registrarPorAdmin(form(2L)));
        assertTrue(e.getMessage().contains("sección A"));
        verify(repository, never()).save(any());
    }

    @Test void trasRetirarseNoVuelveAMatricularseEnElMismoCursoYPeriodo() {
        previa(seccion(1L, 1L, abierto), "RETIRADO");
        ValidacionException e = assertThrows(ValidacionException.class, () -> service.registrarPorAdmin(form(1L)));
        assertTrue(e.getMessage().contains("se retiró"));
    }

    @Test void adminRespetaLasVacantes() {
        CursoPeriodo cp = seccion(1L, 1L, abierto); cp.setVacantes(2);
        when(repository.countByCursoPeriodoIdAndEstado(1L, "ACTIVO")).thenReturn(2L);
        assertThrows(ValidacionException.class, () -> service.registrarPorAdmin(form(1L)));
        verify(repository, never()).save(any());
    }

    // ── Reactivar ──

    @Test void reactivarDevuelveARetiradoAActivoSiHayVacantes() {
        CursoPeriodo cp = seccion(1L, 1L, abierto);
        Matricula m = new Matricula(); m.setEstado("RETIRADO"); m.setCursoPeriodo(cp);
        when(repository.findById(1L)).thenReturn(Optional.of(m));
        service.reactivar(1L);
        assertEquals("ACTIVO", m.getEstado());
    }

    @Test void noSeReactivaSinVacantes() {
        CursoPeriodo cp = seccion(1L, 1L, abierto); cp.setVacantes(1);
        when(repository.countByCursoPeriodoIdAndEstado(1L, "ACTIVO")).thenReturn(1L);
        Matricula m = new Matricula(); m.setEstado("RETIRADO"); m.setCursoPeriodo(cp);
        when(repository.findById(1L)).thenReturn(Optional.of(m));
        assertThrows(IllegalArgumentException.class, () -> service.reactivar(1L));
        assertEquals("RETIRADO", m.getEstado());
    }
}
