package com.academic.controller;

import com.academic.model.PeriodoAcademico;
import com.academic.service.PeriodoAcademicoService;
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
@RequestMapping("/periodos")
public class PeriodoAcademicoController {

    @Autowired
    private PeriodoAcademicoService service;

    @GetMapping
    public String listar(HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/login";
        return vistaLista(model, null);
    }

    // Cualquier error (nombre repetido, fechas incoherentes) vuelve al modal con lo escrito.
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("periodo") PeriodoAcademico periodo, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                periodo.setId(null);
                service.guardar(periodo);
                ra.addFlashAttribute("mensaje", "Periodo registrado correctamente.");
                return "redirect:/periodos";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        return vistaLista(model, "modalNuevo");
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public PeriodoAcademico editar(@PathVariable Long id) {
        return service.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("periodoEditar") PeriodoAcademico periodo, BindingResult result,
                             Model model, RedirectAttributes ra) {
        if (periodo.getId() == null || service.buscarPorId(periodo.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "El periodo no existe.");
            return "redirect:/periodos";
        }
        if (!result.hasErrors()) {
            try {
                service.guardar(periodo);
                ra.addFlashAttribute("mensaje", "Periodo actualizado correctamente.");
                return "redirect:/periodos";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        return vistaLista(model, "modalEditar");
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Periodo eliminado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar el periodo porque tiene secciones registradas.");
        }
        return "redirect:/periodos";
    }

    // ── Estado del periodo y matrícula (solo administrador; el interceptor bloquea los POST del docente) ──

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, RedirectAttributes ra) {
        return accion(ra, () -> service.activar(id), "Periodo activado.");
    }

    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id, RedirectAttributes ra) {
        int[] culminadas = {0};
        String vista = accion(ra, () -> culminadas[0] = service.cerrar(id), null);
        if (!ra.getFlashAttributes().containsKey("error")) {
            ra.addFlashAttribute("mensaje", "Periodo cerrado. Su matrícula quedó cerrada y "
                    + culminadas[0] + " matrícula(s) activa(s) pasaron a CULMINADO.");
        }
        return vista;
    }

    @PostMapping("/{id}/abrir-matricula")
    public String abrirMatricula(@PathVariable Long id, RedirectAttributes ra) {
        return accion(ra, () -> service.abrirMatricula(id), "Matrícula abierta: los estudiantes ya pueden inscribirse.");
    }

    @PostMapping("/{id}/cerrar-matricula")
    public String cerrarMatricula(@PathVariable Long id, RedirectAttributes ra) {
        return accion(ra, () -> service.cerrarMatricula(id), "Matrícula cerrada.");
    }

    private String accion(RedirectAttributes ra, Runnable operacion, String mensaje) {
        try {
            operacion.run();
            if (mensaje != null) ra.addFlashAttribute("mensaje", mensaje);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Otro usuario cambió el estado de los periodos al mismo tiempo. Inténtalo de nuevo.");
        }
        return "redirect:/periodos";
    }

    /** Completa el modelo de la lista; si viene de un formulario con errores, reabre su modal. */
    private String vistaLista(Model model, String abrirModal) {
        model.addAttribute("periodos", service.listarTodos());
        if (!model.containsAttribute("periodo")) model.addAttribute("periodo", new PeriodoAcademico());
        if (!model.containsAttribute("periodoEditar")) model.addAttribute("periodoEditar", new PeriodoAcademico());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "periodos");
        return "periodos/lista";
    }
}
