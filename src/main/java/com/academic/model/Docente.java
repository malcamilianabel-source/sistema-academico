package com.academic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "docente")
@Data
@NoArgsConstructor
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER) // en la BD los id son SERIAL (integer)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "DNI debe tener 8 dígitos")
    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Pattern(regexp = "\\d{9}", message = "Teléfono debe tener 9 dígitos")
    @Column(length = 20, unique = true)
    private String telefono;

    @Column(length = 150)
    private String especialidad;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Ingresa un email válido")
    @Column(nullable = false, unique = true, length = 150)
    private String email;
}
