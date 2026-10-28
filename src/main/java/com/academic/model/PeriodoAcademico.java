package com.academic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "periodo_academico")
@Data
@NoArgsConstructor
public class PeriodoAcademico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @NotBlank(message = "El nombre del periodo es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /** PLANIFICADO → ACTIVO → CERRADO. Solo puede haber un periodo ACTIVO. */
    @Column(nullable = false, length = 20)
    private String estado = ESTADO_PLANIFICADO;

    /** Si los estudiantes pueden matricularse en este periodo. Solo un periodo a la vez. */
    @Column(name = "matricula_abierta", nullable = false)
    private boolean matriculaAbierta = false;

    public static final String ESTADO_PLANIFICADO = "PLANIFICADO";
    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_CERRADO = "CERRADO";

    public boolean isCerrado() {
        return ESTADO_CERRADO.equals(estado);
    }
}
