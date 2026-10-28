package com.academic.dto;

import com.academic.model.Asistencia;
import com.academic.model.Evaluacion;
import com.academic.model.Matricula;

import java.util.List;

/**
 * Detalle de una matrícula para el administrador.
 * La tardanza cuenta como asistencia. El promedio es simple (la ponderación llega en la fase 5).
 */
public record DetalleMatricula(Matricula matricula,
                               List<Asistencia> asistencias,
                               List<Evaluacion> evaluaciones,
                               long presentes,
                               long tardanzas,
                               long ausentes,
                               Double porcentajeAsistencia,
                               Double promedio) {
}
