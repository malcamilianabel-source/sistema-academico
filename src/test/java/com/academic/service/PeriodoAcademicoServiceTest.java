package com.academic.service;

import com.academic.model.PeriodoAcademico;
import com.academic.repository.PeriodoAcademicoRepository;
import com.academic.repository.SesionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PeriodoAcademicoServiceTest {

    @Mock PeriodoAcademicoRepository repository;
    @Mock SesionRepository sesiones;
    @Mock com.academic.repository.MatriculaRepository matriculas;
    // Dependencias del cierre con notas ponderadas (devuelven listas vacías por defecto)
    @Mock com.academic.repository.CursoPeriodoRepository seccionesRepo;
    @Mock com.academic.repository.ComponenteEvaluacionRepository componentesRepo;
    @Mock com.academic.repository.EvaluacionRepository notasRepo;
    @Mock CalculoAcademicoService calculo;
    @InjectMocks PeriodoAcademicoService service;

    @BeforeEach
    void preparar() {
        when(repository.findFirstByNombreIgnoreCase(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(sesiones.contarFueraDeRango(anyLong(), any(), any())).thenReturn(0L);
    }

    private PeriodoAcademico periodo(Long id, String nombre, String inicio, String fin) {
        PeriodoAcademico p = new PeriodoAcademico();
        p.setId(id);
        p.setNombre(nombre);
        p.setFechaInicio(LocalDate.parse(inicio));
        p.setFechaFin(LocalDate.parse(fin));
        return p;
    }

    @Test
    void fechaFinAnteriorAlInicioSeRechazaEnElCampoFechaFin() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.guardar(periodo(null, "2026-I", "2026-07-15", "2026-03-01")));

        assertEquals("fechaFin", e.getCampo());
        assertEquals("La fecha de fin debe ser posterior a la fecha de inicio.", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void fechaFinIgualAlInicioTambienSeRechaza() {
        assertThrows(ValidacionException.class,
                () -> service.guardar(periodo(null, "2026-I", "2026-03-01", "2026-03-01")));
    }

    @Test
    void nombreRepetidoSinImportarMayusculas() {
        when(repository.findFirstByNombreIgnoreCase("2026-i"))
                .thenReturn(Optional.of(periodo(1L, "2026-I", "2026-03-01", "2026-07-15")));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.guardar(periodo(null, "2026-i", "2026-03-01", "2026-07-15")));

        assertEquals("nombre", e.getCampo());
    }

    @Test
    void noPermiteFechasQueDejenSesionesFuera() {
        when(repository.findById(1L)).thenReturn(Optional.of(periodo(1L, "2026-I", "2026-03-01", "2026-07-15")));
        when(sesiones.contarFueraDeRango(1L, LocalDate.parse("2026-04-01"), LocalDate.parse("2026-07-15")))
                .thenReturn(3L);

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> service.guardar(periodo(1L, "2026-I", "2026-04-01", "2026-07-15")));

        assertTrue(e.getMessage().startsWith("Hay 3 sesión(es)"));
    }

    @Test
    void editarNoCambiaElEstadoNiLaMatricula() {
        PeriodoAcademico guardado = periodo(2L, "2026-II", "2026-08-01", "2026-12-15");
        guardado.setEstado(PeriodoAcademico.ESTADO_ACTIVO);
        guardado.setMatriculaAbierta(true);
        when(repository.findById(2L)).thenReturn(Optional.of(guardado));

        // El formulario no envía estado: llega con los valores por defecto
        service.guardar(periodo(2L, "2026-II", "2026-08-03", "2026-12-15"));

        assertEquals(PeriodoAcademico.ESTADO_ACTIVO, guardado.getEstado());
        assertTrue(guardado.isMatriculaAbierta());
        assertEquals(LocalDate.parse("2026-08-03"), guardado.getFechaInicio());
    }

    @Test
    void noSeActivaSiYaHayOtroPeriodoActivo() {
        PeriodoAcademico activo = periodo(1L, "2026-I", "2026-03-01", "2026-07-15");
        activo.setEstado(PeriodoAcademico.ESTADO_ACTIVO);
        when(repository.findFirstByEstado(PeriodoAcademico.ESTADO_ACTIVO)).thenReturn(Optional.of(activo));
        when(repository.findById(2L)).thenReturn(Optional.of(periodo(2L, "2026-II", "2026-08-01", "2026-12-15")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.activar(2L));
        assertTrue(e.getMessage().contains("2026-I sigue activo"));
    }

    @Test
    void cerrarElPeriodoCierraSuMatriculaYCulminaLasActivas() {
        PeriodoAcademico activo = periodo(1L, "2026-I", "2026-03-01", "2026-07-15");
        activo.setEstado(PeriodoAcademico.ESTADO_ACTIVO);
        activo.setMatriculaAbierta(true);
        when(repository.findById(1L)).thenReturn(Optional.of(activo));

        when(matriculas.culminarActivasDelPeriodo(1L)).thenReturn(3);

        assertEquals(3, service.cerrar(1L));
        verify(matriculas).culminarActivasDelPeriodo(1L);
        assertTrue(activo.isCerrado());
        assertFalse(activo.isMatriculaAbierta());
        assertThrows(IllegalArgumentException.class, () -> service.abrirMatricula(1L));
    }

    @Test
    void soloUnPeriodoConMatriculaAbierta() {
        PeriodoAcademico abierto = periodo(1L, "2026-II", "2026-08-01", "2026-12-15");
        abierto.setMatriculaAbierta(true);
        when(repository.findFirstByMatriculaAbiertaTrue()).thenReturn(Optional.of(abierto));
        when(repository.findById(2L)).thenReturn(Optional.of(periodo(2L, "2027-I", "2027-03-01", "2027-07-15")));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.abrirMatricula(2L));
        assertTrue(e.getMessage().contains("ya está abierta en 2026-II"));
    }

    @Test
    void unPeriodoCerradoNoSeEdita() {
        PeriodoAcademico cerrado = periodo(1L, "2026-I", "2026-03-01", "2026-07-15");
        cerrado.setEstado(PeriodoAcademico.ESTADO_CERRADO);
        when(repository.findById(1L)).thenReturn(Optional.of(cerrado));
        assertThrows(IllegalArgumentException.class,
                () -> service.guardar(periodo(1L, "2026-I", "2026-03-02", "2026-07-15")));
    }

    @Test
    void guardaUnPeriodoValidoSinEspaciosSobrantes() {
        PeriodoAcademico guardado = service.guardar(periodo(null, " 2027-I ", "2027-03-01", "2027-07-15"));
        assertEquals("2027-I", guardado.getNombre());
    }
}
