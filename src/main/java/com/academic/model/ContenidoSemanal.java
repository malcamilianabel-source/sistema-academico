package com.academic.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="contenido_semanal",uniqueConstraints=@UniqueConstraint(name="uk_contenido_semana",columnNames={"curso_periodo_id","semana"}))
@Data @NoArgsConstructor
public class ContenidoSemanal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @JdbcTypeCode(SqlTypes.INTEGER)
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="curso_periodo_id",nullable=false)
    private CursoPeriodo cursoPeriodo;
    @NotNull @Min(1) @Max(30) @Column(nullable=false)
    private Integer semana;
    @NotBlank @Size(max=150) @Column(nullable=false,length=150)
    private String titulo;
    @Size(max=1500) @Column(length=1500)
    private String descripcion;
    @Size(max=500) @Column(name="enlace_material",length=500)
    private String enlaceMaterial;
}
