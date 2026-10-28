package com.academic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Matrícula registrada por el administrador: un estudiante en una sección. */
@Data
public class MatriculaForm {

    @NotNull(message = "Selecciona un estudiante")
    private Long estudianteId;

    @NotNull(message = "Selecciona una sección")
    private Long cursoPeriodoId;
}
