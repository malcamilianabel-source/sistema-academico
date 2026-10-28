package com.academic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Datos del formulario de sección (crear y editar). */
@Data
public class SeccionForm {

    private Long id;

    @NotNull(message = "Selecciona un periodo")
    private Long periodoId;

    @NotNull(message = "Selecciona un curso")
    private Long cursoId;

    /** Vacía = se asigna la siguiente letra libre (A, B, C...). */
    private String seccion;

    @NotNull(message = "Ingresa las vacantes")
    @Min(value = 1, message = "Las vacantes deben estar entre 1 y 500")
    @Max(value = 500, message = "Las vacantes deben estar entre 1 y 500")
    private Integer vacantes = 40;

    @NotNull(message = "Selecciona el docente titular")
    private Long docenteId;
}
