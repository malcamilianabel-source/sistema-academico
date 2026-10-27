package com.academic.service;

import com.academic.model.*;
import com.academic.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {
    @Mock MatriculaRepository repository;
    @Mock CursoPeriodoRepository cursos;
    @Mock EstudianteRepository estudiantes;
    @InjectMocks MatriculaService service;
    Estudiante estudiante;

    @BeforeEach void preparar() {
        ReflectionTestUtils.setField(service, "periodoMatricula", "2026-II");
        estudiante = new Estudiante();
        estudiante.setId(7L);
    }

    private CursoPeriodo curso(Long id, String periodo) {
        Curso c = new Curso(); c.setId(id); c.setNombre("Curso " + id);
        PeriodoAcademico p = new PeriodoAcademico(); p.setId(1L); p.setNombre(periodo);
        CursoPeriodo cp = new CursoPeriodo(); cp.setId(id); cp.setCurso(c); cp.setPeriodo(p);
        return cp;
    }

    @Test void registraVariosCursosParaElUsuario() {
        when(estudiantes.buscarPorUsuarioConBloqueo(1L)).thenReturn(Optional.of(estudiante));
        when(cursos.findById(1L)).thenReturn(Optional.of(curso(1L, "2026-II")));
        when(cursos.findById(2L)).thenReturn(Optional.of(curso(2L, "2026-II")));
        service.matricularCursos(1L, List.of(1L, 2L));
        verify(repository, times(2)).save(argThat(m -> m.getEstudiante() == estudiante && "ACTIVO".equals(m.getEstado())));
    }

    @Test void rechazaTodaLaSeleccionSiUnCursoEsDeOtroPeriodo() {
        when(estudiantes.buscarPorUsuarioConBloqueo(1L)).thenReturn(Optional.of(estudiante));
        when(cursos.findById(1L)).thenReturn(Optional.of(curso(1L, "2026-II")));
        when(cursos.findById(2L)).thenReturn(Optional.of(curso(2L, "2026-I")));
        assertThrows(IllegalArgumentException.class, () -> service.matricularCursos(1L, List.of(1L, 2L)));
        verify(repository, never()).save(any());
    }

    @Test void rechazaCursoYaMatriculado() {
        when(estudiantes.buscarPorUsuarioConBloqueo(1L)).thenReturn(Optional.of(estudiante));
        when(cursos.findById(1L)).thenReturn(Optional.of(curso(1L, "2026-II")));
        when(repository.existsByEstudianteIdAndCursoPeriodoCursoIdAndCursoPeriodoPeriodoNombreIgnoreCase(7L, 1L, "2026-II")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.matricularCursos(1L, List.of(1L)));
        verify(repository, never()).save(any());
    }

    @Test void bajaConservaElRegistro() {
        Matricula m = new Matricula(); m.setEstado("ACTIVO");
        when(repository.findById(1L)).thenReturn(Optional.of(m));
        service.darDeBaja(1L);
        assertEquals("RETIRADO", m.getEstado());
        verify(repository).save(m);
        verify(repository, never()).deleteById(any());
    }
}
