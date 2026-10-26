package com.academic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "asistencia", uniqueConstraints = @UniqueConstraint(
        name = "uk_asistencia_matricula_sesion",
        columnNames = {"matricula_id", "sesion_id"}))
@Data
@NoArgsConstructor
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "sesion_id", nullable = false)
    private Sesion sesion;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "PRESENTE|AUSENTE|TARDANZA", message = "El estado debe ser PRESENTE, AUSENTE o TARDANZA")
    @Column(nullable = false, length = 20)
    private String estado;
}
