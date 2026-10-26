package com.academic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Fila del historial académico: una matrícula con el promedio de sus evaluaciones. */
@Getter
@AllArgsConstructor
public class HistorialCurso {

    public static final double NOTA_APROBATORIA = 11.0;

    private Long matriculaId;
    private String codigo;
    private String curso;
    private Integer creditos;
    private String periodo;
    private String docente;
    private String estadoMatricula;
    private Double promedio;
    private Long cantidadNotas;

    public boolean isConNotas() {
        return promedio != null;
    }

    public String getEstadoAcademico() {
        if ("RETIRADO".equals(estadoMatricula)) return "RETIRADO";
        if (promedio == null) return "SIN NOTAS";
        return promedio >= NOTA_APROBATORIA ? "APROBADO" : "DESAPROBADO";
    }
}
