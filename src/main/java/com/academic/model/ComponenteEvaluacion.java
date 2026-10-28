package com.academic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;

@Entity
@Table(name="componente_evaluacion", uniqueConstraints=@UniqueConstraint(name="uk_componente", columnNames={"curso_periodo_id","nombre"}))
@Data @NoArgsConstructor
public class ComponenteEvaluacion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @JdbcTypeCode(SqlTypes.INTEGER)
    private Long id;
    @ManyToOne @JoinColumn(name="curso_periodo_id",nullable=false)
    private CursoPeriodo cursoPeriodo;
    @NotBlank @Column(nullable=false,length=50) private String nombre;
    @NotNull @DecimalMin("0.01") @DecimalMax("100.0") @JdbcTypeCode(SqlTypes.NUMERIC) @Column(nullable=false) private Double peso;
    private LocalDate fecha;
}
