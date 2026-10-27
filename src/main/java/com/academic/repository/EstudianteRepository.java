package com.academic.repository;

import com.academic.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    List<Estudiante> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(
            String nombre, String apellido);
    Optional<Estudiante> findByDni(String dni);
    Optional<Estudiante> findByEmail(String email);
    Optional<Estudiante> findByUsuarioId(Long usuarioId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Estudiante e where e.usuario.id = :usuarioId")
    Optional<Estudiante> buscarPorUsuarioConBloqueo(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
