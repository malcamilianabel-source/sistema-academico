package com.academic.controller;

import com.academic.model.Estudiante;
import com.academic.model.Usuario;
import com.academic.service.EstudianteService;
import com.academic.service.MatriculaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/matriculas")
public class MatriculaController {
    @Autowired private MatriculaService matriculaService;
    @Autowired private EstudianteService estudianteService;

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("paginaActual", "matriculas");
        model.addAttribute("periodoActual", matriculaService.getPeriodoMatricula());
        if ("ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) {
            Estudiante estudiante = estudianteService.buscarPorUsuario(usuario.getId()).orElse(null);
            if (estudiante == null) {
                model.addAttribute("aviso", "Tu usuario no está vinculado a un estudiante. Contacta al administrador.");
            } else {
                model.addAttribute("cursosPeriodo", matriculaService.cursosDisponibles());
                model.addAttribute("matriculas", matriculaService.buscarPorEstudiante(estudiante.getId()));
            }
            return "matriculas/estudiante";
        }
        model.addAttribute("matriculas", matriculaService.listarTodos());
        return "matriculas/lista";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam(value = "cursosIds", required = false) List<Long> cursosIds,
                            HttpSession session, RedirectAttributes ra) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!"ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) return "redirect:/acceso-denegado";
        try {
            matriculaService.matricularCursos(usuario.getId(), cursosIds);
            ra.addFlashAttribute("mensaje", "Los cursos se registraron correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "No se pudo completar la matrícula. Comprueba los cursos seleccionados.");
        }
        return "redirect:/matriculas";
    }

    @PostMapping("/baja/{id}")
    public String baja(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!"ADMIN".equalsIgnoreCase(usuario.getRol())) return "redirect:/acceso-denegado";
        try {
            matriculaService.darDeBaja(id);
            ra.addFlashAttribute("mensaje", "Matrícula dada de baja correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/matriculas";
    }
}
