package com.academic.service;

import com.academic.model.*;
import com.academic.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculoAcademicoServiceTest {
    @Mock ComponenteEvaluacionRepository componentes;
    @Mock EvaluacionRepository calificaciones;
    @InjectMocks CalculoAcademicoService service;

    @Test void ponderaYNormalizaSoloLasNotasPresentes() {
        ComponenteEvaluacion parcial=new ComponenteEvaluacion();parcial.setId(1L);parcial.setPeso(30.0);
        ComponenteEvaluacion finalExamen=new ComponenteEvaluacion();finalExamen.setId(2L);finalExamen.setPeso(70.0);
        Evaluacion nota=new Evaluacion();nota.setComponente(parcial);nota.setNota(16.0);
        when(componentes.findByCursoPeriodoIdOrderByNombreAsc(5L)).thenReturn(List.of(parcial,finalExamen));
        when(calificaciones.findByMatriculaId(10L)).thenReturn(List.of(nota));
        assertEquals(16.0,service.promedio(10L,5L,false));
        assertEquals(4.8,service.promedio(10L,5L,true));
    }

    @Test void sinNotasEsNuloEnCursoYCeroEnCierre() {
        ComponenteEvaluacion c=new ComponenteEvaluacion();c.setId(1L);c.setPeso(100.0);
        when(componentes.findByCursoPeriodoIdOrderByNombreAsc(5L)).thenReturn(List.of(c));
        when(calificaciones.findByMatriculaId(10L)).thenReturn(List.of());
        assertNull(service.promedio(10L,5L,false));
        assertEquals(0.0,service.promedio(10L,5L,true));
    }
}
