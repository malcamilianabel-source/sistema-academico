package com.academic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "curso")
@Data
@NoArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @NotBlank(message = "El nombre del curso es obligatorio")
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 20, message = "El código no puede superar los 20 caracteres")
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Min(value = 1, message = "Los créditos deben estar entre 1 y 10")
    @Max(value = 10, message = "Los créditos deben estar entre 1 y 10")
    private Integer creditos;

    @Min(value = 1, message = "El ciclo debe estar entre 1 y 10")
    @Max(value = 10, message = "El ciclo debe estar entre 1 y 10")
    private Integer ciclo;

    @Min(value = 1, message = "Las horas deben ser al menos 1")
    private Integer horas;
}
