package com.academic.dto;

/** Proyección del query nativo de estudiantes en riesgo académico (promedio menor a 11). */
public interface EstudianteRiesgo {
    Long getId();
    String getNombre();
    String getApellido();
    String getDni();
    Double getPromedio();
    Long getCursosDesaprobados();
}
