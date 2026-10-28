package com.academic.controller;

import com.academic.model.Estudiante;
import com.academic.service.EstudianteService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/estudiantes")
public class EstudianteController {

    @Autowired
    private EstudianteService estudianteService;

    // ── LISTAR ──────────────────────────────────────────────
    @GetMapping
    public String listar(@RequestParam(required = false) String buscar,
                         HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/login";

        List<Estudiante> lista;
        if (buscar != null && !buscar.isBlank()) {
            lista = estudianteService.buscar(buscar);
            model.addAttribute("buscar", buscar);
        } else {
            lista = estudianteService.listarTodos();
        }

        model.addAttribute("estudiantes", lista);
        return vistaLista(model, null);
    }

    // ── CREAR ───────────────────────────────────────────────
    // Cualquier error (formato o dato repetido) vuelve al modal con lo escrito y el campo marcado.
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Estudiante estudiante, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                estudianteService.registrar(estudiante);
                ra.addFlashAttribute("mensaje", "Estudiante guardado correctamente.");
                return "redirect:/estudiantes";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        model.addAttribute("estudiantes", estudianteService.listarTodos());
        return vistaLista(model, "modalNuevo");
    }

    // ── EDITAR (JSON para el modal) ──────────────────────────
    @GetMapping("/editar/{id}")
    @ResponseBody
    public Estudiante obtenerParaEditar(@PathVariable Long id) {
        return estudianteService.buscarPorId(id).orElse(null);
    }

    // ── ACTUALIZAR ───────────────────────────────────────────
    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("estudianteEditar") Estudiante form, BindingResult result,
                             Model model, RedirectAttributes ra) {
        if (form.getId() == null || estudianteService.buscarPorId(form.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "El estudiante no existe.");
            return "redirect:/estudiantes";
        }
        if (!result.hasErrors()) {
            try {
                estudianteService.actualizar(form);
                ra.addFlashAttribute("mensaje", "Estudiante actualizado correctamente.");
                return "redirect:/estudiantes";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        model.addAttribute("estudiantes", estudianteService.listarTodos());
        return vistaLista(model, "modalEditar");
    }

    // ── ELIMINAR ─────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            estudianteService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Estudiante eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar el estudiante porque tiene matrículas registradas.");
        }
        return "redirect:/estudiantes";
    }

    /** Completa el modelo de la lista; si viene de un formulario con errores, reabre su modal. */
    private String vistaLista(Model model, String abrirModal) {
        if (!model.containsAttribute("estudiante")) model.addAttribute("estudiante", new Estudiante());
        if (!model.containsAttribute("estudianteEditar")) model.addAttribute("estudianteEditar", new Estudiante());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "estudiantes");
        return "estudiantes/lista";
    }
}
