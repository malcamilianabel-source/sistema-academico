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

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("periodo") PeriodoAcademico periodo, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return vistaLista(model, "modalNuevo");
        }
        try {
            service.guardar(periodo);
            ra.addFlashAttribute("mensaje", "Periodo registrado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/periodos";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public PeriodoAcademico editar(@PathVariable Long id) {
        return service.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("periodoEditar") PeriodoAcademico periodo, BindingResult result,
                             Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return vistaLista(model, "modalEditar");
        }
        try {
            service.guardar(periodo);
            ra.addFlashAttribute("mensaje", "Periodo actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/periodos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Periodo eliminado correctamente.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar el periodo porque tiene asignaciones registradas.");
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
