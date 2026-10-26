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

    @Query("select s from Sesion s where s.cursoPeriodo.docente.usuario.id = :usuarioId "
            + "order by s.fechaSesion desc, s.horaInicio desc")
    List<Sesion> findByDocenteUsuarioId(@Param("usuarioId") Long usuarioId);
}
