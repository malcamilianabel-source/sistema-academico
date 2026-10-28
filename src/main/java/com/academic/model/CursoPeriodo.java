package com.academic.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sección académica: un curso dictado en un periodo (A, B, C...), con sus vacantes.
 * El nombre de la clase y la tabla se conservan; en pantalla se muestra como "Sección".
 * {@code docente} es siempre el docente TITULAR; los docentes de apoyo están en {@link AsignacionDocente}.
 */
@Entity
@Table(name = "curso_periodo", uniqueConstraints = @UniqueConstraint(
        name = "uk_seccion",
        columnNames = {"curso_id", "periodo_id", "seccion"}))
@Data
@NoArgsConstructor
public class CursoPeriodo {

    public static final int VACANTES_POR_DEFECTO = 40;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    /** Docente titular de la sección. */
    @ManyToOne
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @ManyToOne
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    /** Letra de la sección dentro del curso y periodo: A, B, C... */
    @Column(nullable = false, length = 2)
    private String seccion = "A";

    @Column(nullable = false)
    private Integer vacantes = VACANTES_POR_DEFECTO;

    /** Texto para listas y selectores: "ISW-301 — Programación Web · Sección A · 2026-II". */
    public String getEtiqueta() {
        String texto = curso == null ? "" : curso.getCodigo() + " — " + curso.getNombre();
        texto += " · Sección " + seccion;
        return periodo == null ? texto : texto + " · " + periodo.getNombre();
    }
}
