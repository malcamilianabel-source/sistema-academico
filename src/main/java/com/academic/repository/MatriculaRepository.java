package com.academic.repository;

import com.academic.dto.HistorialCurso;
import com.academic.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByEstudianteId(Long estudianteId);
    List<Matricula> findByCursoPeriodoId(Long cursoPeriodoId);
    List<Matricula> findByCursoPeriodoIdAndEstadoOrderByEstudianteApellidoAsc(Long cursoPeriodoId, String estado);
    long countByEstado(String estado);
    boolean existsByEstudianteIdAndCursoPeriodoCursoIdAndCursoPeriodoPeriodoNombreIgnoreCase(Long estudianteId, Long cursoId, String periodo);

    boolean existsByEstudianteIdAndCursoPeriodoId(Long estudianteId, Long cursoPeriodoId);

    @Query("select m from Matricula m where m.cursoPeriodo.docente.usuario.id = :usuarioId")
    List<Matricula> findByDocenteUsuarioId(@Param("usuarioId") Long usuarioId);

    /** Historial de un estudiante: matrícula + curso_periodo + curso + evaluaciones, con el promedio por matrícula. */
    @Query("select new com.academic.dto.HistorialCurso(m.id, c.codigo, c.nombre, c.creditos, p.nombre, "
            + "concat(d.nombre, ' ', d.apellido), m.estado, avg(e.nota), count(e)) "
            + "from Matricula m join m.cursoPeriodo cp join cp.curso c join cp.periodo p join cp.docente d "
            + "left join Evaluacion e on e.matricula = m "
            + "where m.estudiante.id = :estudianteId "
            + "group by m.id, c.codigo, c.nombre, c.creditos, p.nombre, p.fechaInicio, d.nombre, d.apellido, m.estado "
            + "order by p.fechaInicio desc, c.nombre")
    List<HistorialCurso> findHistorialByEstudianteId(@Param("estudianteId") Long estudianteId);
}
