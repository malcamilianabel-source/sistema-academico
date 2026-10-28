package com.academic.repository;

import com.academic.dto.EstudianteRiesgo;
import com.academic.model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {
    List<Evaluacion> findByMatriculaId(Long matriculaId);
    List<Evaluacion> findByMatriculaIdOrderByFechaAsc(Long matriculaId);

    @Query("select e from Evaluacion e join e.matricula m join m.cursoPeriodo cp where cp.docente.usuario.id = :usuarioId or exists (select a.id from AsignacionDocente a where a.cursoPeriodo = cp and a.docente.usuario.id = :usuarioId)")
    List<Evaluacion> findByDocenteUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Estudiantes con matrículas activas cuyo promedio (media de los promedios de cada curso) es menor a 11.
     */
    @Query(value = """
            SELECT es.id AS id, es.nombre AS nombre, es.apellido AS apellido, es.dni AS dni,
                   AVG(pm.promedio) AS promedio,
                   COUNT(*) FILTER (WHERE pm.promedio < 11) AS "cursosDesaprobados"
            FROM (SELECT m.id, m.estudiante_id, AVG(e.nota) AS promedio
                  FROM matricula m
                  JOIN evaluacion e ON e.matricula_id = m.id
                  WHERE m.estado = 'ACTIVO'
                  GROUP BY m.id, m.estudiante_id) pm
            JOIN estudiante es ON es.id = pm.estudiante_id
            GROUP BY es.id, es.nombre, es.apellido, es.dni
            HAVING AVG(pm.promedio) < 11
            ORDER BY promedio
            """, nativeQuery = true)
    List<EstudianteRiesgo> findEstudiantesEnRiesgo();
}
