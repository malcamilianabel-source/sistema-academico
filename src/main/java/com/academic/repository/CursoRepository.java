package com.academic.repository;

import com.academic.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByNombreContainingIgnoreCase(String nombre);
    Optional<Curso> findByCodigo(String codigo);

    Optional<Curso> findFirstByCodigoIgnoreCase(String codigo);
}
