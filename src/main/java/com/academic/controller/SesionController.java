package com.academic.controller;

import com.academic.model.CursoPeriodo;
import com.academic.model.Sesion;
import com.academic.model.Usuario;
import com.academic.service.AsistenciaService;
import com.academic.service.CursoPeriodoService;
import com.academic.service.SesionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sesiones")
public class SesionController {

    @Autowired private SesionService sesionService;
    @Autowired private CursoPeriodoService cursoPeriodoService;
    @Autowired private AsistenciaService asistenciaService;


    @GetMapping
    public String listar(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        // Un docente solo ve las sesiones de los cursos que dicta
        model.addAttribute("sesiones", "DOCENTE".equalsIgnoreCase(usuario.getRol())
                ? sesionService.listarPorDocente(usuario.getId())
                : sesionService.listarTodos());
        model.addAttribute("asistenciasPorSesion", asistenciaService.contarPorSesion());
        model.addAttribute("paginaActual", "sesiones");
        return "sesiones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Sesion sesion = new Sesion();
        sesion.setCursoPeriodo(new CursoPeriodo());
        sesion.setTipo("PRESENCIAL");
        model.addAttribute("sesion", sesion);
        return vistaFormulario(model, session);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Sesion sesion, BindingResult result,
                          Model model, RedirectAttributes ra, HttpSession session) {
        resolverCursoPeriodo(sesion, result, session);
        if (result.hasErrors()) {
            return vistaFormulario(model, session);
        }
        sesion.setId(null);
        return guardarSesion(sesion, result, model, ra, session, "Sesión registrada correctamente.");
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra, HttpSession session) {
        Sesion sesion = sesionService.buscarPorId(id).orElse(null);
        if (sesion == null) {
            ra.addFlashAttribute("error", "La sesión no existe.");
            return "redirect:/sesiones";
        }
        if (!permitido(session, sesion.getCursoPeriodo())) return "redirect:/acceso-denegado";
        model.addAttribute("sesion", sesion);
        return vistaFormulario(model, session);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute Sesion sesion, BindingResult result,
                             Model model, RedirectAttributes ra, HttpSession session) {
        resolverCursoPeriodo(sesion, result, session);
        if (result.hasErrors()) {
            return vistaFormulario(model, session);
        }
        if (sesion.getId() == null || sesionService.buscarPorId(sesion.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "La sesión no existe.");
            return "redirect:/sesiones";
        }
        if (!permitido(session, sesion.getCursoPeriodo()) || !permitido(session, sesionService.buscarPorId(sesion.getId()).orElseThrow().getCursoPeriodo())) return "redirect:/acceso-denegado";
        return guardarSesion(sesion, result, model, ra, session, "Sesión actualizada correctamente.");
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra, HttpSession session) {
        Sesion sesion = sesionService.buscarPorId(id).orElse(null);
        if (sesion == null || !permitido(session, sesion.getCursoPeriodo())) return "redirect:/acceso-denegado";
        try {
            sesionService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Sesión eliminada correctamente.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar la sesión porque tiene asistencias registradas.");
        }
        return "redirect:/sesiones";
    }

    private String guardarSesion(Sesion sesion, BindingResult result, Model model,
                                 RedirectAttributes ra, HttpSession session, String mensaje) {
        if (!permitido(session, sesion.getCursoPeriodo())) return "redirect:/acceso-denegado";
        try {
            sesionService.guardar(sesion);
        } catch (IllegalArgumentException e) {
            // Horas incompletas o invertidas, o fecha fuera del periodo: el error se muestra en su campo
            Formularios.rechazar(result, e);
            return vistaFormulario(model, session);
        }
        ra.addFlashAttribute("mensaje", mensaje);
        return "redirect:/sesiones";
    }

    /** El select envía cursoPeriodo.id: se reemplaza por la asignación real o se marca el error. */
    private void resolverCursoPeriodo(Sesion sesion, BindingResult result, HttpSession session) {
        Long id = sesion.getCursoPeriodo() != null ? sesion.getCursoPeriodo().getId() : null;
        CursoPeriodo cursoPeriodo = id != null ? cursoPeriodoService.buscarPorId(id).orElse(null) : null;
        if (cursoPeriodo == null) {
            result.rejectValue("cursoPeriodo.id", "NotNull", "Selecciona un curso");
        } else if (!permitido(session, cursoPeriodo)) {
            result.rejectValue("cursoPeriodo.id", "Permiso", "No puedes gestionar esa sección.");
        } else {
            sesion.setCursoPeriodo(cursoPeriodo);
        }
    }

    private String vistaFormulario(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("cursosPeriodo", "DOCENTE".equals(usuario.getRol()) ? cursoPeriodoService.listarDeDocente(usuario.getId()) : cursoPeriodoService.listarTodos());
        model.addAttribute("paginaActual", "sesiones");
        return "sesiones/form";
    }
    private boolean permitido(HttpSession session, CursoPeriodo cp) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && cp != null && ("ADMIN".equals(usuario.getRol()) ||
                ("DOCENTE".equals(usuario.getRol()) && cursoPeriodoService.dictaSeccion(usuario.getId(), cp)));
    }
}

