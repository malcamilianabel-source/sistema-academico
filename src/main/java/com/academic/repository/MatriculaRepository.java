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

    boolean existsByEstudianteIdAndCursoPeriodoCursoIdAndCursoPeriodoPeriodoId(Long estudianteId, Long cursoId, Long periodoId);

    /** Matrícula del estudiante en ese curso y periodo, en cualquier sección y estado. */
    java.util.Optional<Matricula> findFirstByEstudianteIdAndCursoPeriodoCursoIdAndCursoPeriodoPeriodoId(
            Long estudianteId, Long cursoId, Long periodoId);

    long countByCursoPeriodoIdAndEstado(Long cursoPeriodoId, String estado);

    boolean existsByCursoPeriodoId(Long cursoPeriodoId);

    /**
     * Búsqueda del administrador. Cada filtro vacío (null) no se aplica.
     * {@code texto} ya llega en minúsculas y con comodines (%texto%); busca en nombre, apellido y DNI.
     */
    @Query("select m from Matricula m join fetch m.estudiante e join fetch m.cursoPeriodo cp "
            + "join fetch cp.curso c join fetch cp.periodo p "
            + "where (:texto is null or lower(concat(e.nombre, ' ', e.apellido)) like :texto "
            + "       or lower(concat(e.apellido, ' ', e.nombre)) like :texto or e.dni like :texto) "
            + "and (:periodoId is null or p.id = :periodoId) "
            + "and (:cursoId is null or c.id = :cursoId) "
            + "and (:seccionId is null or cp.id = :seccionId) "
            + "and (:estado is null or m.estado = :estado) "
            + "order by p.fechaInicio desc, c.nombre, cp.seccion, e.apellido, e.nombre")
    List<Matricula> filtrar(@Param("texto") String texto, @Param("periodoId") Long periodoId,
                            @Param("cursoId") Long cursoId, @Param("seccionId") Long seccionId,
                            @Param("estado") String estado);

    /** Al cerrar un periodo, sus matrículas activas pasan a CULMINADO. Devuelve cuántas cambiaron. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("update Matricula m set m.estado = 'CULMINADO' where m.estado = 'ACTIVO' "
            + "and m.cursoPeriodo.id in (select cp.id from CursoPeriodo cp where cp.periodo.id = :periodoId)")
    int culminarActivasDelPeriodo(@Param("periodoId") Long periodoId);

    /** Matrículas ACTIVAS por sección: [cursoPeriodoId, total]. */
    @Query("select m.cursoPeriodo.id, count(m) from Matricula m where m.estado = 'ACTIVO' group by m.cursoPeriodo.id")
    List<Object[]> contarActivasPorSeccion();

    /** Matrículas de las secciones donde el docente es titular o de apoyo. */
    @Query("select m from Matricula m join m.cursoPeriodo cp where cp.docente.usuario.id = :usuarioId or exists (select a.id from AsignacionDocente a where a.cursoPeriodo = cp and a.docente.usuario.id = :usuarioId)")
    List<Matricula> findByDocenteUsuarioId(@Param("usuarioId") Long usuarioId);

    /** Historial de un estudiante: matrícula + curso_periodo + curso + evaluaciones, con el promedio por matrícula. */
    @Query("select new com.academic.dto.HistorialCurso(m.id, c.codigo, c.nombre, c.creditos, p.nombre, "
            + "concat(d.nombre, ' ', d.apellido), m.estado, coalesce(m.notaFinal, sum(e.nota * coalesce(ce.peso, 1.0)) / nullif(sum(coalesce(ce.peso, 1.0)), 0)), count(e)) "
            + "from Matricula m join m.cursoPeriodo cp join cp.curso c join cp.periodo p join cp.docente d "
            + "left join Evaluacion e on e.matricula = m left join e.componente ce "
            + "where m.estudiante.id = :estudianteId "
            + "group by m.id, c.codigo, c.nombre, c.creditos, p.nombre, p.fechaInicio, d.nombre, d.apellido, m.estado, m.notaFinal "
            + "order by p.fechaInicio desc, c.nombre")
    List<HistorialCurso> findHistorialByEstudianteId(@Param("estudianteId") Long estudianteId);
}
