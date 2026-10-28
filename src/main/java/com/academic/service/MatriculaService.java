package com.academic.service;

import com.academic.dto.DetalleMatricula;
import com.academic.dto.HistorialCurso;
import com.academic.dto.MatriculaForm;
import com.academic.model.Asistencia;
import com.academic.model.CursoPeriodo;
import com.academic.model.Estudiante;
import com.academic.model.Evaluacion;
import com.academic.model.Matricula;
import com.academic.model.PeriodoAcademico;
import com.academic.repository.AsistenciaRepository;
import com.academic.repository.CursoPeriodoRepository;
import com.academic.repository.EstudianteRepository;
import com.academic.repository.EvaluacionRepository;
import com.academic.repository.MatriculaRepository;
import com.academic.repository.PeriodoAcademicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Matrículas: ACTIVO → RETIRADO (baja) o ACTIVO → CULMINADO (al cerrar el periodo).
 * Reglas comunes al estudiante y al administrador:
 *  - una sola matrícula por curso y periodo (en cualquier sección y estado: tras retirarse no se vuelve a matricular);
 *  - la sección debe tener vacantes (se bloquea mientras se comprueba);
 *  - los periodos cerrados no admiten cambios.
 */
@Service
public class MatriculaService {

    public static final String ACTIVO = "ACTIVO";
    public static final String RETIRADO = "RETIRADO";
    public static final String CULMINADO = "CULMINADO";
    public static final List<String> ESTADOS = List.of(ACTIVO, RETIRADO, CULMINADO);

    @Autowired private MatriculaRepository repository;
    @Autowired private CursoPeriodoRepository cursoPeriodoRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private PeriodoAcademicoRepository periodoRepository;
    @Autowired private AsistenciaRepository asistenciaRepository;
    @Autowired private EvaluacionRepository evaluacionRepository;

    /** Periodo con la matrícula abierta, definido por el administrador en Periodos. */
    public Optional<PeriodoAcademico> periodoAbierto() {
        return periodoRepository.findFirstByMatriculaAbiertaTrue();
    }

    /** Nombre del periodo con matrícula abierta, o null si está cerrada. */
    public String getPeriodoMatricula() {
        return periodoAbierto().map(PeriodoAcademico::getNombre).orElse(null);
    }

    /** Secciones del periodo abierto, por curso y letra. Vacía si la matrícula está cerrada. */
    public List<CursoPeriodo> cursosDisponibles() {
        return periodoAbierto()
                .map(p -> cursoPeriodoRepository.findByPeriodoIdOrderByCursoNombreAscSeccionAsc(p.getId()))
                .orElse(List.of());
    }

    /** Secciones en las que el administrador puede matricular (periodos planificados o activos). */
    public List<CursoPeriodo> seccionesParaMatricular() {
        return cursoPeriodoRepository.findDePeriodosNoCerrados();
    }

    // ---------------------------------------------------------------------------------------------
    // Registro
    // ---------------------------------------------------------------------------------------------

    /** Matrícula múltiple del propio estudiante, en el periodo con la matrícula abierta. Todo o nada. */
    @Transactional
    public void matricularCursos(Long usuarioId, List<Long> cursosIds) {
        Estudiante estudiante = estudianteRepository.buscarPorUsuarioConBloqueo(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Tu usuario no está vinculado a un estudiante."));
        PeriodoAcademico periodo = periodoAbierto()
                .orElseThrow(() -> new IllegalArgumentException("La matrícula no está abierta en este momento."));
        Validaciones.exigir(cursosIds != null && !cursosIds.isEmpty(), "Selecciona al menos un curso.");

        Set<Long> cursosSeleccionados = new HashSet<>();
        List<CursoPeriodo> secciones = new ArrayList<>();
        for (Long id : cursosIds) {
            // Se bloquea la sección para que dos matrículas simultáneas no superen las vacantes
            CursoPeriodo cp = cursoPeriodoRepository.buscarConBloqueo(id)
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los cursos seleccionados no existe."));
            Validaciones.exigir(periodo.getId().equals(cp.getPeriodo().getId()),
                    "Solo puedes matricularte en cursos del periodo " + periodo.getNombre() + ".");
            Validaciones.exigir(cursosSeleccionados.add(cp.getCurso().getId()),
                    "No puedes seleccionar dos secciones del mismo curso.");
            validarSinMatriculaPrevia(estudiante, cp, null, true);
            validarVacantes(cp, null);
            secciones.add(cp);
        }

        for (CursoPeriodo cp : secciones) {
            repository.save(nueva(estudiante, cp));
        }
        repository.flush();
    }

    /**
     * Matrícula registrada por el administrador. Puede hacerlo en cualquier periodo no cerrado,
     * aunque la matrícula de estudiantes no esté abierta (matrícula extemporánea).
     */
    @Transactional
    public Matricula registrarPorAdmin(MatriculaForm form) {
        Estudiante estudiante = estudianteRepository.buscarConBloqueo(form.getEstudianteId())
                .orElseThrow(() -> new ValidacionException("estudianteId", "El estudiante no existe."));
        CursoPeriodo cp = cursoPeriodoRepository.buscarConBloqueo(form.getCursoPeriodoId())
                .orElseThrow(() -> new ValidacionException("cursoPeriodoId", "La sección no existe."));
        Validaciones.exigir(!cp.getPeriodo().isCerrado(), "cursoPeriodoId",
                "El periodo " + cp.getPeriodo().getNombre() + " está cerrado; ya no admite matrículas.");
        validarSinMatriculaPrevia(estudiante, cp, "cursoPeriodoId", false);
        validarVacantes(cp, "cursoPeriodoId");
        Matricula matricula = repository.save(nueva(estudiante, cp));
        repository.flush();
        return matricula;
    }

    private Matricula nueva(Estudiante estudiante, CursoPeriodo cp) {
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setCursoPeriodo(cp);
        matricula.setEstado(ACTIVO);
        return matricula;
    }

    /** Una sola matrícula por curso y periodo, en cualquier sección; tras retirarse no se vuelve a matricular. */
    private void validarSinMatriculaPrevia(Estudiante estudiante, CursoPeriodo cp, String campo, boolean propio) {
        Optional<Matricula> previa = repository.findFirstByEstudianteIdAndCursoPeriodoCursoIdAndCursoPeriodoPeriodoId(
                estudiante.getId(), cp.getCurso().getId(), cp.getPeriodo().getId());
        if (previa.isEmpty()) return;
        Matricula m = previa.get();
        String curso = cp.getCurso().getNombre();
        String mensaje;
        if (RETIRADO.equals(m.getEstado())) {
            mensaje = (propio ? "Te retiraste de " : "El estudiante se retiró de ") + curso + " en este periodo; "
                    + "no se permite volver a matricularse en el mismo curso y periodo.";
        } else {
            mensaje = (propio ? "Ya tienes" : "El estudiante ya tiene") + " una matrícula en " + curso
                    + " (sección " + m.getCursoPeriodo().getSeccion() + ") para este periodo.";
        }
        if (campo == null) throw new IllegalArgumentException(mensaje);
        throw new ValidacionException(campo, mensaje);
    }

    private void validarVacantes(CursoPeriodo cp, String campo) {
        long matriculados = repository.countByCursoPeriodoIdAndEstado(cp.getId(), ACTIVO);
        int vacantes = cp.getVacantes() == null ? 0 : cp.getVacantes();
        String mensaje = "La sección " + cp.getSeccion() + " de " + cp.getCurso().getNombre() + " ya no tiene vacantes.";
        if (campo == null) Validaciones.exigir(matriculados < vacantes, mensaje);
        else Validaciones.exigir(matriculados < vacantes, campo, mensaje);
    }

    // ---------------------------------------------------------------------------------------------
    // Cambios de estado
    // ---------------------------------------------------------------------------------------------

    /** ACTIVO → RETIRADO. Se conserva el registro con sus notas y asistencias. */
    @Transactional
    public void darDeBaja(Long id) {
        Matricula matricula = obtener(id);
        Validaciones.exigir(ACTIVO.equals(matricula.getEstado()), "Solo se puede retirar una matrícula activa.");
        Validaciones.exigir(!matricula.getCursoPeriodo().getPeriodo().isCerrado(),
                "El periodo está cerrado; la matrícula ya no se puede modificar.");
        matricula.setEstado(RETIRADO);
        repository.save(matricula);
    }

    /**
     * RETIRADO → ACTIVO, para corregir un retiro hecho por error. Solo el administrador,
     * con el periodo sin cerrar y si la sección aún tiene vacantes.
     */
    @Transactional
    public void reactivar(Long id) {
        Matricula matricula = obtener(id);
        Validaciones.exigir(RETIRADO.equals(matricula.getEstado()), "Solo se puede reactivar una matrícula retirada.");
        CursoPeriodo cp = cursoPeriodoRepository.buscarConBloqueo(matricula.getCursoPeriodo().getId())
                .orElseThrow(() -> new IllegalArgumentException("La sección no existe."));
        Validaciones.exigir(!cp.getPeriodo().isCerrado(), "El periodo está cerrado; la matrícula ya no se puede modificar.");
        validarVacantes(cp, null);
        matricula.setEstado(ACTIVO);
        repository.save(matricula);
    }

    // ---------------------------------------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------------------------------------

    /** Búsqueda del administrador; los filtros vacíos no se aplican. */
    public List<Matricula> filtrar(String texto, Long periodoId, Long cursoId, Long seccionId, String estado) {
        String patron = Validaciones.vacio(texto) ? null : "%" + texto.trim().toLowerCase() + "%";
        String estadoValido = estado != null && ESTADOS.contains(estado) ? estado : null;
        return repository.filtrar(patron, periodoId, cursoId, seccionId, estadoValido);
    }

    public DetalleMatricula detalle(Long id) {
        Matricula matricula = obtener(id);
        List<Asistencia> asistencias = asistenciaRepository.findByMatriculaIdOrderBySesionFechaSesionAsc(id);
        List<Evaluacion> evaluaciones = evaluacionRepository.findByMatriculaIdOrderByFechaAsc(id);
        long presentes = asistencias.stream().filter(a -> "PRESENTE".equals(a.getEstado())).count();
        long tardanzas = asistencias.stream().filter(a -> "TARDANZA".equals(a.getEstado())).count();
        long ausentes = asistencias.stream().filter(a -> "AUSENTE".equals(a.getEstado())).count();
        Double porcentaje = asistencias.isEmpty() ? null : (presentes + tardanzas) * 100.0 / asistencias.size();
        Double promedio = evaluaciones.stream().filter(e -> e.getNota() != null)
                .mapToDouble(Evaluacion::getNota).average().stream().boxed().findFirst().orElse(null);
        return new DetalleMatricula(matricula, asistencias, evaluaciones, presentes, tardanzas, ausentes,
                porcentaje, promedio);
    }

    private Matricula obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("La matrícula no existe."));
    }

    public List<Matricula> listarTodos() {
        return repository.findAll();
    }

    public Optional<Matricula> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public boolean existeMatricula(Long estudianteId, Long cursoPeriodoId) {
        return repository.existsByEstudianteIdAndCursoPeriodoId(estudianteId, cursoPeriodoId);
    }

    public List<Matricula> listarPorDocente(Long usuarioId) {
        return repository.findByDocenteUsuarioId(usuarioId);
    }

    public List<Matricula> buscarPorEstudiante(Long estudianteId) {
        return repository.findByEstudianteId(estudianteId);
    }

    public long contarTotal() {
        return repository.count();
    }

    public long contarActivas() {
        return repository.countByEstado(ACTIVO);
    }

    public List<Matricula> listarActivasPorCursoPeriodo(Long cursoPeriodoId) {
        return repository.findByCursoPeriodoIdAndEstadoOrderByEstudianteApellidoAsc(cursoPeriodoId, ACTIVO);
    }

    public List<HistorialCurso> historialPorEstudiante(Long estudianteId) {
        return repository.findHistorialByEstudianteId(estudianteId);
    }
}
