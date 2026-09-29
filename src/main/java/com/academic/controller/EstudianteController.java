package com.academic.controller;

import com.academic.model.Estudiante;
import com.academic.service.EstudianteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

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
        model.addAttribute("nuevoEstudiante", new Estudiante());
        model.addAttribute("paginaActual", "estudiantes");
        return "estudiantes/lista";
    }

    // ── CREAR ────────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Estudiante estudiante,
                          RedirectAttributes redirectAttributes) {
        estudianteService.guardar(estudiante);
        redirectAttributes.addFlashAttribute("mensaje", "Estudiante guardado correctamente.");
        return "redirect:/estudiantes";
    }

    // ── EDITAR (cargar datos al modal via JSON) ──────────────
    @GetMapping("/editar/{id}")
    @ResponseBody
    public Estudiante obtenerParaEditar(@PathVariable Long id) {
        return estudianteService.buscarPorId(id).orElse(null);
    }

    // ── ACTUALIZAR ───────────────────────────────────────────
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Estudiante form, RedirectAttributes ra) {
        Estudiante e = estudianteService.buscarPorId(form.getId()).orElseThrow();
        e.setNombre(form.getNombre());
        e.setApellido(form.getApellido());
        e.setDni(form.getDni());
        e.setEmail(form.getEmail());
        e.setTelefono(form.getTelefono());
        e.setFechaInscripcion(form.getFechaInscripcion());
        estudianteService.guardar(e);
        ra.addFlashAttribute("mensaje", "Estudiante actualizado correctamente.");
        return "redirect:/estudiantes";
    }

    // ── ELIMINAR ─────────────────────────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        estudianteService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensaje", "Estudiante eliminado correctamente.");
        return "redirect:/estudiantes";
    }
}
