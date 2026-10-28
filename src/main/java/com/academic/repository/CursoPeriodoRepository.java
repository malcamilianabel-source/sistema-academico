package com.academic.repository;

import com.academic.model.CursoPeriodo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoPeriodoRepository extends JpaRepository<CursoPeriodo, Long> {

    /** Secciones de un periodo, para la matrícula: por curso y letra. */
    List<CursoPeriodo> findByPeriodoIdOrderByCursoNombreAscSeccionAsc(Long periodoId);

    @Query("select cp from CursoPeriodo cp order by cp.periodo.fechaInicio desc, cp.curso.nombre, cp.seccion")
    List<CursoPeriodo> findAllOrdenadas();

    /** Secciones de periodos no cerrados, donde el administrador aún puede matricular. */
    @Query("select cp from CursoPeriodo cp where cp.periodo.estado <> 'CERRADO' "
            + "order by cp.periodo.fechaInicio desc, cp.curso.nombre, cp.seccion")
    List<CursoPeriodo> findDePeriodosNoCerrados();

    Optional<CursoPeriodo> findFirstByCursoIdAndPeriodoIdAndSeccion(Long cursoId, Long periodoId, String seccion);

    List<CursoPeriodo> findByCursoIdAndPeriodoId(Long cursoId, Long periodoId);

    /** Secciones donde el docente (por su usuario) es titular o de apoyo. */
    @Query("select cp from CursoPeriodo cp where cp.docente.usuario.id = :usuarioId or exists ("
            + "select a.id from AsignacionDocente a where a.cursoPeriodo = cp and a.docente.usuario.id = :usuarioId) "
            + "order by cp.periodo.fechaInicio desc, cp.curso.nombre, cp.seccion")
    List<CursoPeriodo> findDeDocente(@Param("usuarioId") Long usuarioId);

    /** Bloquea la sección mientras se comprueban sus vacantes (dos matrículas a la vez no superan el cupo). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cp from CursoPeriodo cp where cp.id = :id")
    Optional<CursoPeriodo> buscarConBloqueo(@Param("id") Long id);
}
