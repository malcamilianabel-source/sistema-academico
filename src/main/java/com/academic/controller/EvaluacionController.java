package com.academic.controller;

import com.academic.model.Evaluacion;
import com.academic.model.Matricula;
import com.academic.model.Usuario;
import com.academic.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/evaluaciones")
public class EvaluacionController {

    private static final String SIN_PERMISO = "Solo puedes gestionar calificaciones de tus propios cursos.";
    private static final String SOLO_ACTIVAS =
            "Solo se pueden registrar o cambiar notas de matrículas activas. Pide al administrador cualquier corrección.";

    @Autowired
    private EvaluacionService evaluacionService;

    @Autowired
    private ComponenteEvaluacionService componentes;
    
    @Autowired
    private MatriculaService matriculaService;
    @Autowired private CursoPeriodoService cursoPeriodoService;

    @Autowired
    private DocenteService docenteService;

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        return vistaLista(usuario, model, null);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Evaluacion evaluacion, BindingResult result,
                          HttpSession session, Model model, RedirectAttributes ra) {
        // El select envía matricula.id
        Long matriculaId = evaluacion.getMatricula() != null ? evaluacion.getMatricula().getId() : null;
        if (matriculaId == null) result.rejectValue("matricula.id", "NotNull", "Selecciona una matrícula");
        if (result.hasErrors()) {
            return vistaLista((Usuario) session.getAttribute("usuario"), model, "modalNuevo");
        }

        Matricula matricula = matriculaService.buscarPorId(matriculaId).orElse(null);
        if (matricula == null) {
            result.rejectValue("matricula.id", "NoExiste", "La matrícula seleccionada no existe.");
            return vistaLista((Usuario) session.getAttribute("usuario"), model, "modalNuevo");
        }
        if (!puedeGestionar(session, matricula)) {
            ra.addFlashAttribute("error", SIN_PERMISO);
            return "redirect:/evaluaciones";
        }
        if (!puedeCalificar(session, matricula)) {
            result.rejectValue("matricula.id", "NoActiva", SOLO_ACTIVAS);
            return vistaLista((Usuario) session.getAttribute("usuario"), model, "modalNuevo");
        }
        try {
            evaluacion.setId(null);
            evaluacion.setMatricula(matricula);
            if (evaluacion.getComponente()!=null && evaluacion.getComponente().getId()!=null) {
                var componente=componentes.buscar(evaluacion.getComponente().getId());
                if(!componente.getCursoPeriodo().getId().equals(matricula.getCursoPeriodo().getId())) {
                    result.rejectValue("componente.id","NoCorresponde","La evaluación no pertenece a esta sección.");
                    return vistaLista((Usuario)session.getAttribute("usuario"),model,"modalNuevo");
                }
                evaluacion.setComponente(componente);evaluacion.setTipoEvaluacion(componente.getNombre());
            }
            if (evaluacion.getFecha() == null) evaluacion.setFecha(LocalDate.now());
            evaluacionService.guardar(evaluacion);
            ra.addFlashAttribute("mensaje", "Calificación registrada correctamente.");
            return "redirect:/evaluaciones";
        } catch (IllegalArgumentException e) {
            Formularios.rechazar(result, e);
            return vistaLista((Usuario) session.getAttribute("usuario"), model, "modalNuevo");
        }
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Evaluacion obtenerParaEditar(@PathVariable Long id, HttpSession session) {
        return evaluacionService.buscarPorId(id)
                .filter(e -> puedeGestionar(session, e.getMatricula()))
                .orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("evaluacionEditar") Evaluacion form, BindingResult result,
                             HttpSession session, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return vistaLista((Usuario) session.getAttribute("usuario"), model, "modalEditar");
        }
        try {
            Evaluacion evaluacion = evaluacionService.buscarPorId(form.getId()).orElseThrow();
            if (!puedeGestionar(session, evaluacion.getMatricula())) {
                ra.addFlashAttribute("error", SIN_PERMISO);
                return "redirect:/evaluaciones";
            }
            if (!puedeCalificar(session, evaluacion.getMatricula())) {
                ra.addFlashAttribute("error", SOLO_ACTIVAS);
                return "redirect:/evaluaciones";
            }
            evaluacion.setTipoEvaluacion(evaluacion.getComponente()!=null ? evaluacion.getComponente().getNombre() : form.getTipoEvaluacion());
            evaluacion.setNota(form.getNota());
            evaluacion.setFecha(form.getFecha());
            evaluacionService.guardar(evaluacion);
            ra.addFlashAttribute("mensaje", "Calificación actualizada correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/evaluaciones";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Evaluacion evaluacion = evaluacionService.buscarPorId(id).orElse(null);
        if (evaluacion != null && !puedeGestionar(session, evaluacion.getMatricula())) {
            ra.addFlashAttribute("error", SIN_PERMISO);
            return "redirect:/evaluaciones";
        }
        if (evaluacion != null && !puedeCalificar(session, evaluacion.getMatricula())) {
            ra.addFlashAttribute("error", SOLO_ACTIVAS);
            return "redirect:/evaluaciones";
        }
        evaluacionService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Calificación eliminada correctamente.");
        return "redirect:/evaluaciones";
    }

    /** Completa el modelo de la lista; si viene de un formulario con errores, reabre su modal. */
    private String vistaLista(Usuario usuario, Model model, String abrirModal) {
        // Un docente solo ve las calificaciones y matrículas de los cursos que dicta
        if (esDocente(usuario)) {
            model.addAttribute("evaluaciones", evaluacionService.listarPorDocente(usuario.getId()));
            // Para registrar notas el docente solo ve matrículas activas
            model.addAttribute("matriculas", matriculaService.listarPorDocente(usuario.getId()).stream()
                    .filter(m -> "ACTIVO".equals(m.getEstado())).toList());
            if (docenteService.buscarPorUsuario(usuario.getId()).isEmpty()) {
                model.addAttribute("aviso", "Tu usuario no está vinculado a ningún docente. "
                        + "Pide al administrador que registre un docente con tu mismo correo.");
            }
        } else {
            model.addAttribute("evaluaciones", evaluacionService.listarTodos());
            model.addAttribute("matriculas", matriculaService.listarTodos());
        }
        if (!model.containsAttribute("evaluacion")) {
            Evaluacion nueva = new Evaluacion();
            nueva.setMatricula(new Matricula());
            nueva.setComponente(new com.academic.model.ComponenteEvaluacion());
            model.addAttribute("evaluacion", nueva);
        }
        if (!model.containsAttribute("evaluacionEditar")) model.addAttribute("evaluacionEditar", new Evaluacion());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("componentesDisponibles", componentes.todos());
        model.addAttribute("seccionesDisponibles", esDocente(usuario) ? cursoPeriodoService.listarDeDocente(usuario.getId()) : cursoPeriodoService.listarTodos());
        model.addAttribute("paginaActual", "evaluaciones");
        return "evaluaciones/lista";
    }

    private boolean esDocente(Usuario usuario) {
        return "DOCENTE".equalsIgnoreCase(usuario.getRol());
    }

    /**
     * El docente solo registra, cambia o borra notas de matrículas ACTIVAS; las de matrículas retiradas
     * o culminadas forman parte del historial. El administrador puede corregirlas.
     */
    private boolean puedeCalificar(HttpSession session, Matricula matricula) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return false;
        return !esDocente(usuario) || ("ACTIVO".equals(matricula.getEstado()) && !matricula.getCursoPeriodo().getPeriodo().isCerrado());
    }

    /** El administrador gestiona cualquier calificación; el docente, solo las de sus cursos. */
    private boolean puedeGestionar(HttpSession session, Matricula matricula) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return false;
        if (!esDocente(usuario)) return true;
        // Titular o docente de apoyo de la sección
        return cursoPeriodoService.dictaSeccion(usuario.getId(), matricula.getCursoPeriodo());
    }
}
