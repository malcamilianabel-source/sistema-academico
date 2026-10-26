package com.academic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sesion")
@Data
@NoArgsConstructor
public class Sesion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @NotNull(message = "Selecciona un curso")
    @ManyToOne
    @JoinColumn(name = "curso_periodo_id", nullable = false)
    private CursoPeriodo cursoPeriodo;

    @NotNull(message = "La fecha de la sesión es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_sesion", nullable = false)
    private LocalDate fechaSesion;

    @DateTimeFormat(pattern = "HH:mm")
    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @DateTimeFormat(pattern = "HH:mm")
    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @NotBlank(message = "El aula es obligatoria")
    @Size(max = 50, message = "El aula no puede superar los 50 caracteres")
    @Column(nullable = false, length = 50)
    private String aula;

    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(regexp = "PRESENCIAL|VIRTUAL", message = "El tipo debe ser PRESENCIAL o VIRTUAL")
    @Column(nullable = false, length = 20)
    private String tipo;
}
