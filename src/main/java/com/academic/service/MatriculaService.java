package com.academic.service;

import com.academic.dto.HistorialCurso;
import com.academic.model.Matricula;
import com.academic.repository.MatriculaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MatriculaService {

    private static final Set<String> ESTADOS = Set.of("ACTIVO", "RETIRADO", "CULMINADO");

    @Autowired
    private MatriculaRepository repository;
    @Autowired private com.academic.repository.CursoPeriodoRepository cursoPeriodoRepository;
    @Autowired private com.academic.repository.EstudianteRepository estudianteRepository;

    @org.springframework.beans.factory.annotation.Value("${academico.periodo-matricula:2026-II}")
    private String periodoMatricula;

    public String getPeriodoMatricula() {
        return periodoMatricula;
    }

    public List<com.academic.model.CursoPeriodo> cursosDisponibles() {
        return cursoPeriodoRepository.findByPeriodoNombreIgnoreCaseOrderByCursoNombreAsc(periodoMatricula);
    }

    @org.springframework.transaction.annotation.Transactional
    public void matricularCursos(Long usuarioId, List<Long> cursosIds) {
        com.academic.model.Estudiante estudiante = estudianteRepository.buscarPorUsuarioConBloqueo(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Tu usuario no está vinculado a un estudiante."));
        Validaciones.exigir(cursosIds != null && !cursosIds.isEmpty(), "Selecciona al menos un curso.");
        Set<Long> cursosSeleccionados = new java.util.HashSet<>();
        List<com.academic.model.CursoPeriodo> asignaciones = new java.util.ArrayList<>();
        for (Long id : cursosIds) {
            com.academic.model.CursoPeriodo cp = cursoPeriodoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los cursos seleccionados no existe."));
            Validaciones.exigir(periodoMatricula.equalsIgnoreCase(cp.getPeriodo().getNombre()),
                    "Solo puedes matricularte en cursos del periodo " + periodoMatricula + ".");
            Validaciones.exigir(cursosSeleccionados.add(cp.getCurso().getId()),
                    "No puedes seleccionar el mismo curso más de una vez.");
            Validaciones.exigir(!repository.existsByEstudianteIdAndCursoPeriodoCursoIdAndCursoPeriodoPeriodoNombreIgnoreCase(
                            estudiante.getId(), cp.getCurso().getId(), periodoMatricula),
                    "Ya tienes una matrícula registrada en " + cp.getCurso().getNombre() + " para este periodo.");
            asignaciones.add(cp);
        }
        for (com.academic.model.CursoPeriodo cp : asignaciones) {
            Matricula matricula = new Matricula();
            matricula.setEstudiante(estudiante);
            matricula.setCursoPeriodo(cp);
            matricula.setEstado("ACTIVO");
            repository.save(matricula);
        }
        repository.flush();
    }

    @org.springframework.transaction.annotation.Transactional
    public void darDeBaja(Long id) {
        Matricula matricula = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La matrícula no existe."));
        Validaciones.exigir("ACTIVO".equals(matricula.getEstado()), "Solo se puede dar de baja una matrícula activa.");
        matricula.setEstado("RETIRADO");
        repository.save(matricula);
    }

    public List<Matricula> listarTodos() {
        return repository.findAll();
    }

    public Optional<Matricula> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Matricula guardar(Matricula matricula) {
        Validaciones.exigir(matricula.getEstado() != null && ESTADOS.contains(matricula.getEstado()),
                "El estado debe ser ACTIVO, RETIRADO o CULMINADO.");
        return repository.save(matricula);
    }

    public boolean existeMatricula(Long estudianteId, Long cursoPeriodoId) {
        return repository.existsByEstudianteIdAndCursoPeriodoId(estudianteId, cursoPeriodoId);
    }

    public List<Matricula> listarPorDocente(Long usuarioId) {
        return repository.findByDocenteUsuarioId(usuarioId);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public List<Matricula> buscarPorEstudiante(Long estudianteId) {
        return repository.findByEstudianteId(estudianteId);
    }

    public long contarTotal() {
        return repository.count();
    }

    public long contarActivas() {
        return repository.countByEstado("ACTIVO");
    }

    public List<Matricula> listarActivasPorCursoPeriodo(Long cursoPeriodoId) {
        return repository.findByCursoPeriodoIdAndEstadoOrderByEstudianteApellidoAsc(cursoPeriodoId, "ACTIVO");
    }

    public List<HistorialCurso> historialPorEstudiante(Long estudianteId) {
        return repository.findHistorialByEstudianteId(estudianteId);
    }
}
