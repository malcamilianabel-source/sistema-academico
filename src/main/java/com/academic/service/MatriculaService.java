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
