package com.academic.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "curso_periodo", uniqueConstraints = @UniqueConstraint(
        name = "uk_curso_periodo_asignacion",
        columnNames = {"curso_id", "docente_id", "periodo_id"}))
@Data
@NoArgsConstructor
public class CursoPeriodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @ManyToOne
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;
}
