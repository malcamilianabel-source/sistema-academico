package com.academic.controller;

import com.academic.model.Asistencia;
import com.academic.model.Matricula;
import com.academic.model.Sesion;
import com.academic.model.Usuario;
import com.academic.service.AsistenciaService;
import com.academic.service.CursoPeriodoService;
import com.academic.service.MatriculaService;
import com.academic.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/asistencias")
public class AsistenciaController {

    private static final String SIN_PERMISO = "Solo puedes registrar la asistencia de tus propios cursos.";
    private static final String PREFIJO = "estado_";

    @Autowired private AsistenciaService asistenciaService;
    @Autowired private SesionService sesionService;
    @Autowired private MatriculaService matriculaService;
    @Autowired private CursoPeriodoService cursoPeriodoService;

    /** Sesiones disponibles para tomar asistencia, con cuántos registros tiene cada una. */
    @GetMapping
    public String listar(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        model.addAttribute("sesiones", esDocente(usuario)
                ? sesionService.listarPorDocente(usuario.getId())
                : sesionService.listarTodos());
        model.addAttribute("asistenciasPorSesion", asistenciaService.contarPorSesion());
        model.addAttribute("paginaActual", "asistencias");
        return "asistencias/lista";
    }

    /** Planilla de la sesión: un estudiante por fila (matrículas activas del curso). */
    @GetMapping("/sesion/{id}")
    public String registro(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        Sesion sesion = sesionService.buscarPorId(id).orElse(null);
        if (sesion == null) {
            ra.addFlashAttribute("error", "La sesión no existe.");
            return "redirect:/asistencias";
        }
        if (!puedeGestionar(session, sesion)) {
            ra.addFlashAttribute("error", SIN_PERMISO);
            return "redirect:/asistencias";
        }

        Map<Long, String> estados = asistenciaService.listarPorSesion(id).stream()
                .collect(Collectors.toMap(a -> a.getMatricula().getId(), Asistencia::getEstado));
        model.addAttribute("sesion", sesion);
        model.addAttribute("matriculas", matriculaService.listarActivasPorCursoPeriodo(sesion.getCursoPeriodo().getId()));
        model.addAttribute("estados", estados);
        model.addAttribute("yaRegistrada", !estados.isEmpty());
        model.addAttribute("paginaActual", "asistencias");
        return "asistencias/registro";
    }

    /** Registro masivo: recibe un parámetro estado_{matriculaId} por estudiante. */
    @PostMapping("/sesion/{id}")
    public String registrar(@PathVariable Long id, @RequestParam Map<String, String> params,
                            HttpSession session, RedirectAttributes ra) {
        Sesion sesion = sesionService.buscarPorId(id).orElse(null);
        if (sesion == null) {
            ra.addFlashAttribute("error", "La sesión no existe.");
            return "redirect:/asistencias";
        }
        if (!puedeGestionar(session, sesion)) {
            ra.addFlashAttribute("error", SIN_PERMISO);
            return "redirect:/asistencias";
        }

        List<Matricula> matriculas = matriculaService.listarActivasPorCursoPeriodo(sesion.getCursoPeriodo().getId());
        Map<Long, String> estados = new HashMap<>();
        for (Matricula matricula : matriculas) {
            String estado = params.get(PREFIJO + matricula.getId());
            if (estado == null || !AsistenciaService.ESTADOS.contains(estado)) {
                ra.addFlashAttribute("error", "Marca la asistencia de todos los estudiantes "
                        + "(PRESENTE, AUSENTE o TARDANZA).");
                return "redirect:/asistencias/sesion/" + id;
            }
            estados.put(matricula.getId(), estado);
        }

        asistenciaService.registrarMasivo(sesion, matriculas, estados);
        ra.addFlashAttribute("mensaje", "Asistencia registrada para " + estados.size() + " estudiante(s).");
        return "redirect:/asistencias/sesion/" + id;
    }

    private boolean esDocente(Usuario usuario) {
        return "DOCENTE".equalsIgnoreCase(usuario.getRol());
    }

    /** El administrador gestiona cualquier sesión; el docente, solo las de sus cursos. */
    private boolean puedeGestionar(HttpSession session, Sesion sesion) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return false;
        if (!esDocente(usuario)) return true;
        // Titular o docente de apoyo de la sección
        return cursoPeriodoService.dictaSeccion(usuario.getId(), sesion.getCursoPeriodo());
    }
}
