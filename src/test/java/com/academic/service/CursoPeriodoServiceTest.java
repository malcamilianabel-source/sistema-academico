package com.academic.service;

import com.academic.dto.SeccionForm;
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
class CursoPeriodoServiceTest {

    @Mock CursoPeriodoRepository repository;
    @Mock AsignacionDocenteRepository asignaciones;
    @Mock CursoRepository cursos;
    @Mock DocenteRepository docentes;
    @Mock PeriodoAcademicoRepository periodos;
    @Mock MatriculaRepository matriculas;
    @Mock SesionRepository sesiones;
    @InjectMocks CursoPeriodoService service;

    PeriodoAcademico periodo;
    Curso curso;
    Docente titular;
    Docente otro;

    @BeforeEach
    void preparar() {
        periodo = new PeriodoAcademico(); periodo.setId(1L); periodo.setNombre("2026-II");
        curso = new Curso(); curso.setId(1L); curso.setNombre("Programación Web");
        titular = docente(1L, "Carlos"); otro = docente(2L, "Rosa");
        when(periodos.findById(1L)).thenReturn(Optional.of(periodo));
        when(cursos.findById(1L)).thenReturn(Optional.of(curso));
        when(docentes.findById(1L)).thenReturn(Optional.of(titular));
        when(docentes.findById(2L)).thenReturn(Optional.of(otro));
        when(repository.findFirstByCursoIdAndPeriodoIdAndSeccion(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Docente docente(Long id, String nombre) {
        Docente d = new Docente(); d.setId(id); d.setNombre(nombre); d.setApellido("Pérez");
        return d;
    }

    private SeccionForm form(String letra) {
        SeccionForm f = new SeccionForm();
        f.setPeriodoId(1L); f.setCursoId(1L); f.setDocenteId(1L); f.setSeccion(letra); f.setVacantes(35);
        return f;
    }

    private CursoPeriodo seccionGuardada(String letra) {
        CursoPeriodo cp = new CursoPeriodo();
        cp.setId(10L); cp.setCurso(curso); cp.setPeriodo(periodo); cp.setSeccion(letra); cp.setDocente(titular);
        when(repository.findById(10L)).thenReturn(Optional.of(cp));
        return cp;
    }

    @Test
    void creaLaSiguienteLetraLibreYRegistraAlTitular() {
        CursoPeriodo existente = new CursoPeriodo(); existente.setSeccion("A");
        when(repository.findByCursoIdAndPeriodoId(1L, 1L)).thenReturn(List.of(existente));

        CursoPeriodo creada = service.crear(form(""));

        assertEquals("B", creada.getSeccion());
        assertEquals(35, creada.getVacantes());
        assertSame(titular, creada.getDocente());
        verify(asignaciones).save(argThat(a -> a.isTitular() && a.getDocente() == titular));
    }

    @Test
    void noRepiteUnaSeccionDelMismoCursoYPeriodo() {
        CursoPeriodo existente = seccionGuardada("A");
        when(repository.findFirstByCursoIdAndPeriodoIdAndSeccion(1L, 1L, "A")).thenReturn(Optional.of(existente));

        ValidacionException e = assertThrows(ValidacionException.class, () -> service.crear(form("a")));

        assertEquals("seccion", e.getCampo());
        verify(repository, never()).save(any());
    }

    @Test
    void noCreaSeccionesEnUnPeriodoCerrado() {
        periodo.setEstado(PeriodoAcademico.ESTADO_CERRADO);
        ValidacionException e = assertThrows(ValidacionException.class, () -> service.crear(form("A")));
        assertEquals("periodoId", e.getCampo());
    }

    @Test
    void nuevoTitularPasaAlAnteriorComoApoyo() {
        CursoPeriodo cp = seccionGuardada("A");
        AsignacionDocente anterior = new AsignacionDocente(cp, titular, AsignacionDocente.TITULAR);
        when(asignaciones.findFirstByCursoPeriodoIdAndRol(10L, AsignacionDocente.TITULAR)).thenReturn(Optional.of(anterior));
        when(asignaciones.findFirstByCursoPeriodoIdAndDocenteId(10L, 2L)).thenReturn(Optional.empty());

        service.agregarDocente(10L, 2L, AsignacionDocente.TITULAR);

        assertEquals(AsignacionDocente.APOYO, anterior.getRol());
        assertSame(otro, cp.getDocente());
        verify(asignaciones).save(argThat(a -> a.isTitular() && a.getDocente() == otro));
    }

    @Test
    void noAsignaDosVecesAlMismoDocente() {
        CursoPeriodo cp = seccionGuardada("A");
        when(asignaciones.findFirstByCursoPeriodoIdAndRol(10L, AsignacionDocente.TITULAR))
                .thenReturn(Optional.of(new AsignacionDocente(cp, titular, AsignacionDocente.TITULAR)));
        when(asignaciones.findFirstByCursoPeriodoIdAndDocenteId(10L, 2L))
                .thenReturn(Optional.of(new AsignacionDocente(cp, otro, AsignacionDocente.APOYO)));

        assertThrows(IllegalArgumentException.class, () -> service.agregarDocente(10L, 2L, AsignacionDocente.APOYO));
    }

    @Test
    void noSeQuitaAlTitular() {
        CursoPeriodo cp = seccionGuardada("A");
        AsignacionDocente asignacion = new AsignacionDocente(cp, titular, AsignacionDocente.TITULAR);
        asignacion.setId(5L);
        when(asignaciones.findById(5L)).thenReturn(Optional.of(asignacion));

        assertThrows(IllegalArgumentException.class, () -> service.quitarDocente(10L, 5L));
        verify(asignaciones, never()).delete(any());
    }

    @Test
    void lasVacantesNoBajanDeLosMatriculados() {
        seccionGuardada("A");
        when(matriculas.countByCursoPeriodoIdAndEstado(10L, "ACTIVO")).thenReturn(38L);
        SeccionForm f = form("A"); f.setId(10L); f.setVacantes(30);

        ValidacionException e = assertThrows(ValidacionException.class, () -> service.actualizar(f));
        assertEquals("vacantes", e.getCampo());
    }
}
