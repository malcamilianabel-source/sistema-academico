package com.academic.controller;

import com.academic.service.CursoService;
import com.academic.service.DocenteService;
import com.academic.service.EstudianteService;
import com.academic.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private DocenteService docenteService;

    @Autowired
    private CursoService cursoService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) {
            return "redirect:/login";
        }
        model.addAttribute("totalEstudiantes", estudianteService.contarTotal());
        model.addAttribute("totalDocentes", docenteService.contarTotal());
        model.addAttribute("totalCursos", cursoService.contarTotal());
        model.addAttribute("totalUsuarios", usuarioService.contarTotal());
        model.addAttribute("paginaActual", "dashboard");
        return "dashboard";
    }
}
