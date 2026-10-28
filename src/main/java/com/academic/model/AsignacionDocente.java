package com.academic.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Docente asignado a una sección, como TITULAR (uno por sección) o de APOYO. */
@Entity
@Table(name = "asignacion_docente", uniqueConstraints = @UniqueConstraint(
        name = "uk_asignacion_docente",
        columnNames = {"curso_periodo_id", "docente_id"}))
@Data
@NoArgsConstructor
public class AsignacionDocente {

    public static final String TITULAR = "TITULAR";
    public static final String APOYO = "APOYO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "curso_periodo_id", nullable = false)
    private CursoPeriodo cursoPeriodo;

    @ManyToOne
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @Column(nullable = false, length = 20)
    private String rol = APOYO;

    public AsignacionDocente(CursoPeriodo cursoPeriodo, Docente docente, String rol) {
        this.cursoPeriodo = cursoPeriodo;
        this.docente = docente;
        this.rol = rol;
    }

    public boolean isTitular() {
        return TITULAR.equals(rol);
    }
}
