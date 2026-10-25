package com.academic.controller;

import com.academic.model.CursoPeriodo;
import com.academic.model.Estudiante;
import com.academic.model.Matricula;
import com.academic.service.CursoPeriodoService;
import com.academic.service.EstudianteService;
import com.academic.service.MatriculaService;
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
@RequestMapping("/matriculas")
public class MatriculaController {

    @Autowired private MatriculaService matriculaService;
    @Autowired private EstudianteService estudianteService;
    @Autowired private CursoPeriodoService cursoPeriodoService;

    @GetMapping
    public String listar(HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/login";
        return vistaLista(model, null);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Matricula matricula, BindingResult result,
                          Model model, RedirectAttributes ra) {
        // Los selects envían estudiante.id y cursoPeriodo.id
        Long estudianteId = matricula.getEstudiante() != null ? matricula.getEstudiante().getId() : null;
        Long cursoPeriodoId = matricula.getCursoPeriodo() != null ? matricula.getCursoPeriodo().getId() : null;
        if (estudianteId == null) result.rejectValue("estudiante.id", "NotNull", "Selecciona un estudiante");
        if (cursoPeriodoId == null) result.rejectValue("cursoPeriodo.id", "NotNull", "Selecciona un curso-periodo");
        if (result.hasErrors()) {
            return vistaLista(model, "modalNuevo");
        }

        if (matriculaService.existeMatricula(estudianteId, cursoPeriodoId)) {
            ra.addFlashAttribute("error", "El estudiante ya está matriculado en ese curso y periodo.");
            return "redirect:/matriculas";
        }
        matricula.setId(null);
        matricula.setEstudiante(estudianteService.buscarPorId(estudianteId).orElseThrow());
        matricula.setCursoPeriodo(cursoPeriodoService.buscarPorId(cursoPeriodoId).orElseThrow());
        try {
            matriculaService.guardar(matricula);
            ra.addFlashAttribute("mensaje", "Matrícula registrada correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/matriculas";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Matricula obtenerParaEditar(@PathVariable Long id) {
        return matriculaService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("matriculaEditar") Matricula form, BindingResult result,
                             Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return vistaLista(model, "modalEditar");
        }
        Matricula matricula = matriculaService.buscarPorId(form.getId()).orElseThrow();
        matricula.setEstado(form.getEstado());
        try {
            matriculaService.guardar(matricula);
            ra.addFlashAttribute("mensaje", "Matrícula actualizada correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/matriculas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            matriculaService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Matrícula eliminada correctamente.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar la matrícula porque tiene calificaciones o asistencias registradas.");
        }
        return "redirect:/matriculas";
    }

    /** Completa el modelo de la lista; si viene de un formulario con errores, reabre su modal. */
    private String vistaLista(Model model, String abrirModal) {
        model.addAttribute("matriculas", matriculaService.listarTodos());
        model.addAttribute("estudiantes", estudianteService.listarTodos());
        model.addAttribute("cursosPeriodo", cursoPeriodoService.listarTodos());
        if (!model.containsAttribute("matricula")) {
            Matricula nueva = new Matricula();
            nueva.setEstudiante(new Estudiante());
            nueva.setCursoPeriodo(new CursoPeriodo());
            model.addAttribute("matricula", nueva);
        }
        if (!model.containsAttribute("matriculaEditar")) model.addAttribute("matriculaEditar", new Matricula());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "matriculas");
        return "matriculas/lista";
    }
}
