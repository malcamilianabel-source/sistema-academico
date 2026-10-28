package com.academic.service;

import com.academic.dto.SeccionForm;
import com.academic.model.AsignacionDocente;
import com.academic.model.Curso;
import com.academic.model.CursoPeriodo;
import com.academic.model.Docente;
import com.academic.model.PeriodoAcademico;
import com.academic.repository.AsignacionDocenteRepository;
import com.academic.repository.CursoPeriodoRepository;
import com.academic.repository.CursoRepository;
import com.academic.repository.DocenteRepository;
import com.academic.repository.MatriculaRepository;
import com.academic.repository.PeriodoAcademicoRepository;
import com.academic.repository.SesionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Secciones (CursoPeriodo) y sus docentes. Cada sección tiene un solo TITULAR, que además se guarda
 * en {@code curso_periodo.docente_id} para que historial, sesiones y pantallas existentes sigan funcionando.
 */
@Service
public class CursoPeriodoService {

    private static final String LETRA = "^[A-Z]{1,2}$";

    @Autowired private CursoPeriodoRepository repository;
    @Autowired private AsignacionDocenteRepository asignacionRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private DocenteRepository docenteRepository;
    @Autowired private PeriodoAcademicoRepository periodoRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private SesionRepository sesionRepository;

    public List<CursoPeriodo> listarTodos() {
        return repository.findAllOrdenadas();
    }

    /** Secciones de un periodo, o todas si periodoId es null. */
    public List<CursoPeriodo> listar(Long periodoId) {
        return periodoId == null ? listarTodos() : repository.findByPeriodoIdOrderByCursoNombreAscSeccionAsc(periodoId);
    }

    /** Secciones donde el docente es titular o de apoyo. */
    public List<CursoPeriodo> listarDeDocente(Long usuarioId) {
        return repository.findDeDocente(usuarioId);
    }

    public Optional<CursoPeriodo> buscarPorId(Long id) {
        return repository.findById(id);
    }

    /** ¿El usuario docente dicta esta sección (como titular o de apoyo)? */
    public boolean dictaSeccion(Long usuarioId, CursoPeriodo seccion) {
        if (usuarioId == null || seccion == null) return false;
        var titular = seccion.getDocente() == null ? null : seccion.getDocente().getUsuario();
        if (titular != null && usuarioId.equals(titular.getId())) return true;
        return asignacionRepository.existsByCursoPeriodoIdAndDocenteUsuarioId(seccion.getId(), usuarioId);
    }

    /** Docentes de cada sección (titular primero). Si una sección antigua no tiene filas, se muestra su titular. */
    public Map<Long, List<AsignacionDocente>> docentesPorSeccion(List<CursoPeriodo> secciones) {
        if (secciones.isEmpty()) return Map.of();
        Map<Long, List<AsignacionDocente>> mapa = asignacionRepository
                .findDeSecciones(secciones.stream().map(CursoPeriodo::getId).toList()).stream()
                .collect(Collectors.groupingBy(a -> a.getCursoPeriodo().getId(), LinkedHashMap::new, Collectors.toList()));
        for (CursoPeriodo cp : secciones) {
            mapa.computeIfAbsent(cp.getId(),
                    k -> List.of(new AsignacionDocente(cp, cp.getDocente(), AsignacionDocente.TITULAR)));
        }
        return mapa;
    }

    /** Matrículas ACTIVAS por sección. */
    public Map<Long, Long> matriculadosPorSeccion() {
        return matriculaRepository.contarActivasPorSeccion().stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> (Long) f[1]));
    }

    @Transactional
    public CursoPeriodo crear(SeccionForm form) {
        PeriodoAcademico periodo = periodoRepository.findById(form.getPeriodoId())
                .orElseThrow(() -> new ValidacionException("periodoId", "Selecciona un periodo válido."));
        Validaciones.exigir(!periodo.isCerrado(), "periodoId",
                "El periodo " + periodo.getNombre() + " está cerrado; no se pueden crear secciones.");
        Curso curso = cursoRepository.findById(form.getCursoId())
                .orElseThrow(() -> new ValidacionException("cursoId", "Selecciona un curso válido."));
        Docente docente = docenteRepository.findById(form.getDocenteId())
                .orElseThrow(() -> new ValidacionException("docenteId", "Selecciona un docente válido."));

        String letra = Validaciones.vacio(form.getSeccion())
                ? siguienteLetra(curso.getId(), periodo.getId())
                : form.getSeccion().trim().toUpperCase();
        validarLetra(letra, curso, periodo, null);
        validarVacantes(form.getVacantes(), 0);

        CursoPeriodo seccion = new CursoPeriodo();
        seccion.setCurso(curso);
        seccion.setPeriodo(periodo);
        seccion.setSeccion(letra);
        seccion.setVacantes(form.getVacantes());
        seccion.setDocente(docente);
        repository.save(seccion);
        asignacionRepository.save(new AsignacionDocente(seccion, docente, AsignacionDocente.TITULAR));
        return seccion;
    }

    /** Cambia la letra y las vacantes. Curso y periodo no cambian: la sección ya puede tener matrículas. */
    @Transactional
    public CursoPeriodo actualizar(SeccionForm form) {
        CursoPeriodo seccion = repository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("La sección no existe."));
        Validaciones.exigir(!seccion.getPeriodo().isCerrado(),
                "El periodo " + seccion.getPeriodo().getNombre() + " está cerrado; la sección ya no se puede modificar.");
        String letra = Validaciones.vacio(form.getSeccion()) ? "" : form.getSeccion().trim().toUpperCase();
        validarLetra(letra, seccion.getCurso(), seccion.getPeriodo(), seccion.getId());
        validarVacantes(form.getVacantes(),
                matriculaRepository.countByCursoPeriodoIdAndEstado(seccion.getId(), "ACTIVO"));
        seccion.setSeccion(letra);
        seccion.setVacantes(form.getVacantes());
        return repository.save(seccion);
    }

    /**
     * Agrega un docente a la sección. Si entra como TITULAR, el titular anterior pasa a APOYO.
     */
    @Transactional
    public void agregarDocente(Long seccionId, Long docenteId, String rol) {
        CursoPeriodo seccion = repository.findById(seccionId)
                .orElseThrow(() -> new IllegalArgumentException("La sección no existe."));
        Validaciones.exigir(!seccion.getPeriodo().isCerrado(), "El periodo está cerrado; no se pueden cambiar docentes.");
        Docente docente = docenteId == null ? null : docenteRepository.findById(docenteId).orElse(null);
        Validaciones.exigir(docente != null, "Selecciona un docente válido.");
        Validaciones.exigir(AsignacionDocente.TITULAR.equals(rol) || AsignacionDocente.APOYO.equals(rol),
                "El rol debe ser TITULAR o APOYO.");
        asegurarFilaDelTitular(seccion);
        Optional<AsignacionDocente> existente = asignacionRepository.findFirstByCursoPeriodoIdAndDocenteId(seccionId, docenteId);
        // Un docente de apoyo puede pasar a titular; cualquier otra repetición es un duplicado
        boolean promover = existente.isPresent() && !existente.get().isTitular() && AsignacionDocente.TITULAR.equals(rol);
        Validaciones.exigir(existente.isEmpty() || promover,
                docente.getNombre() + " " + docente.getApellido() + " ya está asignado a esta sección.");

        if (AsignacionDocente.TITULAR.equals(rol)) {
            asignacionRepository.findFirstByCursoPeriodoIdAndRol(seccionId, AsignacionDocente.TITULAR)
                    .ifPresent(anterior -> {
                        anterior.setRol(AsignacionDocente.APOYO);
                        asignacionRepository.saveAndFlush(anterior);
                    });
            seccion.setDocente(docente);
            repository.save(seccion);
        }
        if (promover) {
            existente.get().setRol(AsignacionDocente.TITULAR);
            asignacionRepository.save(existente.get());
        } else {
            asignacionRepository.save(new AsignacionDocente(seccion, docente, rol));
        }
    }

    /** Quita un docente de apoyo. El titular no se quita: primero se asigna otro titular. */
    @Transactional
    public void quitarDocente(Long seccionId, Long asignacionId) {
        AsignacionDocente asignacion = asignacionRepository.findById(asignacionId)
                .filter(a -> a.getCursoPeriodo().getId().equals(seccionId))
                .orElseThrow(() -> new IllegalArgumentException("La asignación no existe."));
        Validaciones.exigir(!asignacion.getCursoPeriodo().getPeriodo().isCerrado(),
                "El periodo está cerrado; no se pueden cambiar docentes.");
        Validaciones.exigir(!asignacion.isTitular(),
                "No se puede quitar al titular. Asigna primero otro docente como titular.");
        asignacionRepository.delete(asignacion);
    }

    /** Solo se elimina una sección sin matrículas ni sesiones. */
    @Transactional
    public void eliminar(Long id) {
        CursoPeriodo seccion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La sección no existe."));
        Validaciones.exigir(!matriculaRepository.existsByCursoPeriodoId(id),
                "No se puede eliminar la sección porque tiene matrículas registradas.");
        Validaciones.exigir(!sesionRepository.existsByCursoPeriodoId(id),
                "No se puede eliminar la sección porque tiene sesiones registradas.");
        asignacionRepository.eliminarDeSeccion(id);
        repository.delete(seccion);
        repository.flush();
    }

    /** Siguiente letra libre para el curso en el periodo: A, B, C... */
    public String siguienteLetra(Long cursoId, Long periodoId) {
        Set<String> usadas = repository.findByCursoIdAndPeriodoId(cursoId, periodoId).stream()
                .map(CursoPeriodo::getSeccion).collect(Collectors.toSet());
        for (char c = 'A'; c <= 'Z'; c++) {
            if (!usadas.contains(String.valueOf(c))) return String.valueOf(c);
        }
        throw new ValidacionException("seccion", "Ya no quedan letras libres para este curso en el periodo.");
    }

    private void validarLetra(String letra, Curso curso, PeriodoAcademico periodo, Long excluirId) {
        Validaciones.exigir(letra.matches(LETRA), "seccion", "La sección debe ser una o dos letras (A, B, C...).");
        Validaciones.exigir(repository.findFirstByCursoIdAndPeriodoIdAndSeccion(curso.getId(), periodo.getId(), letra)
                        .filter(s -> !s.getId().equals(excluirId)).isEmpty(), "seccion",
                "Ya existe la sección " + letra + " de " + curso.getNombre() + " en " + periodo.getNombre() + ".");
    }

    private void validarVacantes(Integer vacantes, long matriculados) {
        Validaciones.exigir(vacantes != null && vacantes >= 1 && vacantes <= 500, "vacantes",
                "Las vacantes deben estar entre 1 y 500.");
        Validaciones.exigir(vacantes >= matriculados, "vacantes",
                "La sección ya tiene " + matriculados + " matriculados; las vacantes no pueden ser menos.");
    }

    /** Las secciones creadas antes de la migración o por la versión anterior pueden no tener su fila de titular. */
    private void asegurarFilaDelTitular(CursoPeriodo seccion) {
        if (asignacionRepository.findFirstByCursoPeriodoIdAndRol(seccion.getId(), AsignacionDocente.TITULAR).isEmpty()
                && !asignacionRepository.existsByCursoPeriodoIdAndDocenteId(seccion.getId(), seccion.getDocente().getId())) {
            asignacionRepository.saveAndFlush(
                    new AsignacionDocente(seccion, seccion.getDocente(), AsignacionDocente.TITULAR));
        }
    }
}
