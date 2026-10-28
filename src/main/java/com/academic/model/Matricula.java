package com.academic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "matricula", uniqueConstraints = @UniqueConstraint(
        name = "uk_matricula_estudiante_curso_periodo",
        columnNames = {"estudiante_id", "curso_periodo_id"}))
@Data
@NoArgsConstructor
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne
    @JoinColumn(name = "curso_periodo_id", nullable = false)
    private CursoPeriodo cursoPeriodo;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "nota_final")
    private Double notaFinal;

    @Column(name = "fecha_matricula")
    private LocalDate fechaMatricula;

    @NotBlank(message = "El estado es obligatorio")
    @Column(length = 20)
    private String estado = "ACTIVO";

    @PrePersist
    public void prePersist() {
        if (fechaMatricula == null) fechaMatricula = LocalDate.now();
        if (estado == null || estado.isBlank()) estado = "ACTIVO";
    }
}
