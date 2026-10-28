package com.academic.service;

import com.academic.model.PeriodoAcademico;
import com.academic.repository.PeriodoAcademicoRepository;
import com.academic.repository.SesionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.academic.model.PeriodoAcademico.*;

/**
 * Periodos académicos: PLANIFICADO → ACTIVO → CERRADO.
 * Solo hay un periodo ACTIVO y solo uno con la matrícula abierta (la base también lo exige).
 */
@Service
public class PeriodoAcademicoService {

    @Autowired
    private PeriodoAcademicoRepository repository;

    @Autowired
    private SesionRepository sesionRepository;

    @Autowired
    private com.academic.repository.MatriculaRepository matriculaRepository;

    @Autowired private com.academic.repository.CursoPeriodoRepository secciones;
    @Autowired private com.academic.repository.ComponenteEvaluacionRepository componentes;
    @Autowired private com.academic.repository.EvaluacionRepository notas;
    @Autowired private CalculoAcademicoService calculo;

    /** Del más reciente al más antiguo. */
    public List<PeriodoAcademico> listarTodos() {
        return repository.findAllByOrderByFechaInicioDesc();
    }

    public Optional<PeriodoAcademico> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Optional<PeriodoAcademico> periodoActivo() {
        return repository.findFirstByEstado(ESTADO_ACTIVO);
    }

    /** Periodo en el que los estudiantes pueden matricularse, si hay alguno abierto. */
    public Optional<PeriodoAcademico> periodoConMatriculaAbierta() {
        return repository.findFirstByMatriculaAbiertaTrue();
    }

    /**
     * Crea o actualiza nombre y fechas. El estado y la matrícula solo cambian con sus acciones
     * (activar, cerrar, abrir/cerrar matrícula), nunca desde el formulario.
     */
    @Transactional
    public PeriodoAcademico guardar(PeriodoAcademico datos) {
        datos.setNombre(Validaciones.limpiar(datos.getNombre()));
        String nombre = datos.getNombre();
        LocalDate inicio = datos.getFechaInicio();
        LocalDate fin = datos.getFechaFin();

        Validaciones.exigir(Validaciones.coincide(nombre, "^[\\w\\s\\-.]{2,50}$"), "nombre",
                "Ingresa un nombre de periodo válido (de 2 a 50 caracteres).");
        Validaciones.exigir(repository.findFirstByNombreIgnoreCase(nombre)
                        .filter(p -> !p.getId().equals(datos.getId())).isEmpty(),
                "nombre", "Ya existe un periodo con el nombre " + nombre + ".");
        Validaciones.exigir(inicio != null, "fechaInicio", "La fecha de inicio es obligatoria.");
        Validaciones.exigir(fin != null, "fechaFin", "La fecha de fin es obligatoria.");
        Validaciones.exigir(fin.isAfter(inicio), "fechaFin",
                "La fecha de fin debe ser posterior a la fecha de inicio.");

        if (datos.getId() == null) {
            PeriodoAcademico nuevo = new PeriodoAcademico();
            nuevo.setNombre(nombre);
            nuevo.setFechaInicio(inicio);
            nuevo.setFechaFin(fin);
            return repository.save(nuevo);
        }

        PeriodoAcademico actual = repository.findById(datos.getId())
                .orElseThrow(() -> new IllegalArgumentException("El periodo no existe."));
        Validaciones.exigir(!actual.isCerrado(), "El periodo " + actual.getNombre()
                + " está cerrado; ya no se puede modificar.");
        long fuera = sesionRepository.contarFueraDeRango(actual.getId(), inicio, fin);
        Validaciones.exigir(fuera == 0, "fechaFin", "Hay " + fuera
                + " sesión(es) de clase fuera de estas fechas. Ajusta las fechas o mueve esas sesiones.");
        actual.setNombre(nombre);
        actual.setFechaInicio(inicio);
        actual.setFechaFin(fin);
        return repository.save(actual);
    }

    /** PLANIFICADO → ACTIVO. Si ya hay otro activo, primero hay que cerrarlo. */
    @Transactional
    public PeriodoAcademico activar(Long id) {
        PeriodoAcademico periodo = obtener(id);
        Validaciones.exigir(ESTADO_PLANIFICADO.equals(periodo.getEstado()),
                "Solo se puede activar un periodo planificado.");
        periodoActivo().ifPresent(otro -> Validaciones.exigir(false,
                "El periodo " + otro.getNombre() + " sigue activo. Ciérralo antes de activar otro."));
        periodo.setEstado(ESTADO_ACTIVO);
        return repository.save(periodo);
    }

    /**
     * ACTIVO → CERRADO. También cierra su matrícula y pasa sus matrículas activas a CULMINADO
     * (las retiradas se conservan como RETIRADO). Devuelve cuántas matrículas culminaron.
     */
    @Transactional
    public int cerrar(Long id) {
        PeriodoAcademico periodo = obtener(id);
        Validaciones.exigir(ESTADO_ACTIVO.equals(periodo.getEstado()), "Solo se puede cerrar el periodo activo.");
        // Validar todas las ponderaciones ANTES de cerrar, sin dejar cambios parciales.
        for(var cp:secciones.findByPeriodoIdOrderByCursoNombreAscSeccionAsc(id)){
            var lista=componentes.findByCursoPeriodoIdOrderByNombreAsc(cp.getId());
            if(!lista.isEmpty()){
                double peso=lista.stream().mapToDouble(com.academic.model.ComponenteEvaluacion::getPeso).sum();
                Validaciones.exigir(Math.abs(peso-100.0)<0.001,
                  "Los pesos de "+cp.getCurso().getNombre()+" sección "+cp.getSeccion()+" deben sumar 100 %.");
            }
        }
        // Congelar el promedio antes del cambio a CULMINADO.
        for(var cp:secciones.findByPeriodoIdOrderByCursoNombreAscSeccionAsc(id)){
            for(var m:matriculaRepository.findByCursoPeriodoId(cp.getId())){
                if(!"ACTIVO".equals(m.getEstado()))continue;
                var lista=componentes.findByCursoPeriodoIdOrderByNombreAsc(cp.getId());
                Double valor;
                if(lista.isEmpty()){
                    var existentes=notas.findByMatriculaId(m.getId());
                    valor=existentes.isEmpty()?0.0:existentes.stream().mapToDouble(e->e.getNota()).average().orElse(0.0);
                }else valor=calculo.promedio(m.getId(),cp.getId(),true);
                m.setNotaFinal(Math.floor(valor+0.5));
                matriculaRepository.save(m);
            }
        }
        periodo.setEstado(ESTADO_CERRADO);
        periodo.setMatriculaAbierta(false);
        repository.save(periodo);
        return matriculaRepository.culminarActivasDelPeriodo(id);
    }

    /** Habilita la matrícula de estudiantes en este periodo (planificado o activo). */
    @Transactional
    public PeriodoAcademico abrirMatricula(Long id) {
        PeriodoAcademico periodo = obtener(id);
        Validaciones.exigir(!periodo.isCerrado(), "No se puede abrir la matrícula de un periodo cerrado.");
        periodoConMatriculaAbierta().filter(otro -> !otro.getId().equals(id)).ifPresent(otro ->
                Validaciones.exigir(false, "La matrícula ya está abierta en " + otro.getNombre()
                        + ". Ciérrala antes de abrir otra."));
        periodo.setMatriculaAbierta(true);
        return repository.save(periodo);
    }

    @Transactional
    public PeriodoAcademico cerrarMatricula(Long id) {
        PeriodoAcademico periodo = obtener(id);
        periodo.setMatriculaAbierta(false);
        return repository.save(periodo);
    }

    /** Solo se elimina un periodo planificado; si tiene secciones, la base lo impide. */
    @Transactional
    public void eliminar(Long id) {
        PeriodoAcademico periodo = obtener(id);
        Validaciones.exigir(ESTADO_PLANIFICADO.equals(periodo.getEstado()),
                "Solo se puede eliminar un periodo planificado.");
        repository.delete(periodo);
        repository.flush();
    }

    public long contarTotal() {
        return repository.count();
    }

    private PeriodoAcademico obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("El periodo no existe."));
    }
}
