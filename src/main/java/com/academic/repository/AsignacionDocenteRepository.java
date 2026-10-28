package com.academic.repository;

import com.academic.model.AsignacionDocente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, Long> {

    /** Docentes de varias secciones; el titular primero. */
    @Query("select a from AsignacionDocente a where a.cursoPeriodo.id in :ids "
            + "order by case when a.rol = 'TITULAR' then 0 else 1 end, a.docente.apellido")
    List<AsignacionDocente> findDeSecciones(@Param("ids") Collection<Long> ids);

    Optional<AsignacionDocente> findFirstByCursoPeriodoIdAndRol(Long cursoPeriodoId, String rol);

    boolean existsByCursoPeriodoIdAndDocenteId(Long cursoPeriodoId, Long docenteId);

    Optional<AsignacionDocente> findFirstByCursoPeriodoIdAndDocenteId(Long cursoPeriodoId, Long docenteId);

    boolean existsByCursoPeriodoIdAndDocenteUsuarioId(Long cursoPeriodoId, Long usuarioId);

    @Modifying
    @Query("delete from AsignacionDocente a where a.cursoPeriodo.id = :id")
    void eliminarDeSeccion(@Param("id") Long cursoPeriodoId);
}
