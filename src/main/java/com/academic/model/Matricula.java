package com.academic.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "matricula")
@Data
@NoArgsConstructor
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne
    @JoinColumn(name = "curso_periodo_id", nullable = false)
    private CursoPeriodo cursoPeriodo;

    @Column(name = "fecha_matricula")
    private LocalDate fechaMatricula;

    @Column(length = 20)
    private String estado = "ACTIVO";

    @PrePersist
    public void prePersist() {
        if (fechaMatricula == null) fechaMatricula = LocalDate.now();
        if (estado == null || estado.isBlank()) estado = "ACTIVO";
    }
}
