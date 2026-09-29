package com.academic.controller;

import com.academic.model.Matricula;
import com.academic.service.CursoPeriodoService;
import com.academic.service.EstudianteService;
import com.academic.service.MatriculaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
        model.addAttribute("matriculas", matriculaService.listarTodos());
        model.addAttribute("estudiantes", estudianteService.listarTodos());
        model.addAttribute("cursosPeriodo", cursoPeriodoService.listarTodos());
        model.addAttribute("paginaActual", "matriculas");
        return "matriculas/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Long estudianteId,
                          @RequestParam Long cursoPeriodoId,
                          @RequestParam(required = false) String estado,
                          RedirectAttributes ra) {
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudianteService.buscarPorId(estudianteId).orElseThrow());
        matricula.setCursoPeriodo(cursoPeriodoService.buscarPorId(cursoPeriodoId).orElseThrow());
        matricula.setEstado(estado == null || estado.isBlank() ? "ACTIVO" : estado);
        matriculaService.guardar(matricula);
        ra.addFlashAttribute("mensaje", "Matrícula registrada correctamente.");
        return "redirect:/matriculas";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Matricula obtenerParaEditar(@PathVariable Long id) {
        return matriculaService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@RequestParam Long id,
                             @RequestParam String estado,
                             RedirectAttributes ra) {
        Matricula matricula = matriculaService.buscarPorId(id).orElseThrow();
        matricula.setEstado(estado);
        matriculaService.guardar(matricula);
        ra.addFlashAttribute("mensaje", "Matrícula actualizada correctamente.");
        return "redirect:/matriculas";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        matriculaService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Matrícula eliminada correctamente.");
        return "redirect:/matriculas";
    }
}
