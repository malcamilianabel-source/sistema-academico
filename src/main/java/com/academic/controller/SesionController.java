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
    public String nuevo(Model model) {
        Sesion sesion = new Sesion();
        sesion.setCursoPeriodo(new CursoPeriodo());
        sesion.setTipo("PRESENCIAL");
        model.addAttribute("sesion", sesion);
        return vistaFormulario(model);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Sesion sesion, BindingResult result,
                          Model model, RedirectAttributes ra) {
        resolverCursoPeriodo(sesion, result);
        if (result.hasErrors()) {
            return vistaFormulario(model);
        }
        sesion.setId(null);
        return guardarSesion(sesion, result, model, ra, "Sesión registrada correctamente.");
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Sesion sesion = sesionService.buscarPorId(id).orElse(null);
        if (sesion == null) {
            ra.addFlashAttribute("error", "La sesión no existe.");
            return "redirect:/sesiones";
        }
        model.addAttribute("sesion", sesion);
        return vistaFormulario(model);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute Sesion sesion, BindingResult result,
                             Model model, RedirectAttributes ra) {
        resolverCursoPeriodo(sesion, result);
        if (result.hasErrors()) {
            return vistaFormulario(model);
        }
        if (sesion.getId() == null || sesionService.buscarPorId(sesion.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "La sesión no existe.");
            return "redirect:/sesiones";
        }
        return guardarSesion(sesion, result, model, ra, "Sesión actualizada correctamente.");
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
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
                                 RedirectAttributes ra, String mensaje) {
        try {
            sesionService.guardar(sesion);
        } catch (IllegalArgumentException e) {
            // La única regla del servicio es el orden de las horas
            result.rejectValue("horaFin", "HoraFin", e.getMessage());
            return vistaFormulario(model);
        }
        ra.addFlashAttribute("mensaje", mensaje);
        return "redirect:/sesiones";
    }

    /** El select envía cursoPeriodo.id: se reemplaza por la asignación real o se marca el error. */
    private void resolverCursoPeriodo(Sesion sesion, BindingResult result) {
        Long id = sesion.getCursoPeriodo() != null ? sesion.getCursoPeriodo().getId() : null;
        CursoPeriodo cursoPeriodo = id != null ? cursoPeriodoService.buscarPorId(id).orElse(null) : null;
        if (cursoPeriodo == null) {
            result.rejectValue("cursoPeriodo.id", "NotNull", "Selecciona un curso");
        } else {
            sesion.setCursoPeriodo(cursoPeriodo);
        }
    }

    private String vistaFormulario(Model model) {
        model.addAttribute("cursosPeriodo", cursoPeriodoService.listarTodos());
        model.addAttribute("paginaActual", "sesiones");
        return "sesiones/form";
    }
}
