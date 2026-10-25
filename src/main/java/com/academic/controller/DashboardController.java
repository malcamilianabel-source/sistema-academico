package com.academic.controller;

import com.academic.model.Usuario;
import com.academic.service.CursoService;
import com.academic.service.DocenteService;
import com.academic.service.EstudianteService;
import com.academic.service.EvaluacionService;
import com.academic.service.MatriculaService;
import com.academic.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @Autowired private EstudianteService estudianteService;
    @Autowired private DocenteService docenteService;
    @Autowired private CursoService cursoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private MatriculaService matriculaService;
    @Autowired private EvaluacionService evaluacionService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        model.addAttribute("totalEstudiantes", estudianteService.contarTotal());
        model.addAttribute("totalDocentes", docenteService.contarTotal());
        model.addAttribute("totalCursos", cursoService.contarTotal());
        model.addAttribute("totalUsuarios", usuarioService.contarTotal());
        model.addAttribute("matriculasActivas", matriculaService.contarActivas());
        // La lista nominal de estudiantes en riesgo es solo para el personal (no para estudiantes)
        if (!"ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) {
            model.addAttribute("estudiantesRiesgo", evaluacionService.listarEstudiantesEnRiesgo());
        }
        model.addAttribute("paginaActual", "dashboard");
        return "dashboard";
    }
}
