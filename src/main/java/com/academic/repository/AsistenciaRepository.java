package com.academic.repository;

import com.academic.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    List<Asistencia> findBySesionId(Long sesionId);
    @Query("select a.sesion.id, count(a) from Asistencia a group by a.sesion.id")
    List<Object[]> contarAgrupadoPorSesion();
}
