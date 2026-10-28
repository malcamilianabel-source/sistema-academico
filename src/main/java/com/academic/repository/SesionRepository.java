package com.academic.repository;

import com.academic.model.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {
    @Query("select s from Sesion s order by s.fechaSesion desc, s.horaInicio desc")
    List<Sesion> findAllOrdenadas();

    @Query("select s from Sesion s join s.cursoPeriodo cp where cp.docente.usuario.id = :usuarioId or exists (select a.id from AsignacionDocente a where a.cursoPeriodo = cp and a.docente.usuario.id = :usuarioId) "
            + "order by s.fechaSesion desc, s.horaInicio desc")
    List<Sesion> findByDocenteUsuarioId(@Param("usuarioId") Long usuarioId);

    boolean existsByCursoPeriodoId(Long cursoPeriodoId);

    /** Sesiones de un periodo cuya fecha quedaría fuera del rango indicado. */
    @Query("select count(s) from Sesion s where s.cursoPeriodo.periodo.id = :periodoId "
            + "and (s.fechaSesion < :inicio or s.fechaSesion > :fin)")
    long contarFueraDeRango(@Param("periodoId") Long periodoId,
                            @Param("inicio") java.time.LocalDate inicio,
                            @Param("fin") java.time.LocalDate fin);
}
