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
@Table(name = "evaluacion")
@Data
@NoArgsConstructor
public class Evaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @NotBlank(message = "El tipo de evaluación es obligatorio")
    @Column(name = "tipo_evaluacion", length = 50)
    private String tipoEvaluacion;

    @NotNull(message = "La nota es obligatoria")
    @DecimalMin(value = "0.0", message = "La nota mínima es 0")
    @DecimalMax(value = "20.0", message = "La nota máxima es 20")
    @JdbcTypeCode(SqlTypes.NUMERIC) // en la BD la columna es NUMERIC
    private Double nota;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;
}
