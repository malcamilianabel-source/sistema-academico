package com.academic.repository;

import com.academic.model.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, Long> {

    java.util.Optional<PeriodoAcademico> findFirstByNombreIgnoreCase(String nombre);

    java.util.Optional<PeriodoAcademico> findFirstByEstado(String estado);

    java.util.Optional<PeriodoAcademico> findFirstByMatriculaAbiertaTrue();

    java.util.List<PeriodoAcademico> findAllByOrderByFechaInicioDesc();
}
