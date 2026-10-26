package com.academic.service;

import com.academic.model.Asistencia;
import com.academic.model.Matricula;
import com.academic.model.Sesion;
import com.academic.repository.AsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AsistenciaService {

    public static final Set<String> ESTADOS = Set.of("PRESENTE", "AUSENTE", "TARDANZA");

    @Autowired
    private AsistenciaRepository repository;

    public List<Asistencia> listar() {
        return repository.findAll();
    }

    public List<Asistencia> listarPorSesion(Long sesionId) {
        return repository.findBySesionId(sesionId);
    }

    /** Cantidad de asistencias registradas por sesión (sesiónId -> total). */
    public Map<Long, Long> contarPorSesion() {
        return repository.contarAgrupadoPorSesion().stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]));
    }

    public Optional<Asistencia> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Asistencia guardar(Asistencia asistencia) {
        Validaciones.exigir(asistencia.getEstado() != null && ESTADOS.contains(asistencia.getEstado()),
                "El estado debe ser PRESENTE, AUSENTE o TARDANZA.");
        return repository.save(asistencia);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    /**
     * Registra la asistencia de toda la sesión de una vez: crea o actualiza
     * el registro de cada matrícula según el estado elegido (matrículaId -> estado).
     */
    @Transactional
    public void registrarMasivo(Sesion sesion, List<Matricula> matriculas, Map<Long, String> estados) {
        Map<Long, Asistencia> existentes = repository.findBySesionId(sesion.getId()).stream()
                .collect(Collectors.toMap(a -> a.getMatricula().getId(), Function.identity()));

        for (Matricula matricula : matriculas) {
            String estado = estados.get(matricula.getId());
            if (estado == null) continue;
            Asistencia asistencia = existentes.get(matricula.getId());
            if (asistencia == null) {
                asistencia = new Asistencia();
                asistencia.setSesion(sesion);
                asistencia.setMatricula(matricula);
            }
            asistencia.setEstado(estado);
            guardar(asistencia);
        }
    }
}
