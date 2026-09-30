package com.academic.repository;

import com.academic.model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {
    List<Docente> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(
            String nombre, String apellido);
    Optional<Docente> findByDni(String dni);
    Optional<Docente> findByEmail(String email);
    Optional<Docente> findByTelefono(String telefono);
}
