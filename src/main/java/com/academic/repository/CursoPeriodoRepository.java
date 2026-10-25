package com.academic.repository;

import com.academic.model.CursoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CursoPeriodoRepository extends JpaRepository<CursoPeriodo, Long> {
    Optional<CursoPeriodo> findFirstByCursoIdAndDocenteIdAndPeriodoId(Long cursoId, Long docenteId, Long periodoId);
}
