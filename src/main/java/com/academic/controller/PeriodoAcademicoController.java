package com.academic.controller;

import com.academic.model.PeriodoAcademico;
import com.academic.service.PeriodoAcademicoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
        model.addAttribute("periodos", service.listarTodos());
        model.addAttribute("nuevoPeriodo", new PeriodoAcademico());
        model.addAttribute("paginaActual", "periodos");
        return "periodos/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute PeriodoAcademico periodo, RedirectAttributes ra) {
        service.guardar(periodo);
        ra.addFlashAttribute("mensaje", "Periodo registrado correctamente.");
        return "redirect:/periodos";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public PeriodoAcademico editar(@PathVariable Long id) {
        return service.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute PeriodoAcademico periodo, RedirectAttributes ra) {
        service.guardar(periodo);
        ra.addFlashAttribute("mensaje", "Periodo actualizado correctamente.");
        return "redirect:/periodos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        service.eliminar(id);
        ra.addFlashAttribute("mensaje", "Periodo eliminado correctamente.");
        return "redirect:/periodos";
    }
}
