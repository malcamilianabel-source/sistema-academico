package com.academic.controller;

import com.academic.model.Curso;
import com.academic.service.CursoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/cursos")
public class CursoController {

    @Autowired
    private CursoService cursoService;

    @GetMapping
    public String listar(@RequestParam(required = false) String buscar,
                         HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/login";

        List<Curso> lista;
        if (buscar != null && !buscar.isBlank()) {
            lista = cursoService.buscar(buscar);
            model.addAttribute("buscar", buscar);
        } else {
            lista = cursoService.listarTodos();
        }
        model.addAttribute("cursos", lista);
        model.addAttribute("nuevoCurso", new Curso());
        model.addAttribute("paginaActual", "cursos");
        return "cursos/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Curso curso,
                          RedirectAttributes redirectAttributes) {
        cursoService.guardar(curso);
        redirectAttributes.addFlashAttribute("mensaje", "Curso guardado correctamente.");
        return "redirect:/cursos";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Curso obtenerParaEditar(@PathVariable Long id) {
        return cursoService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Curso curso,
                             RedirectAttributes redirectAttributes) {
        cursoService.guardar(curso);
        redirectAttributes.addFlashAttribute("mensaje", "Curso actualizado correctamente.");
        return "redirect:/cursos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        cursoService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensaje", "Curso eliminado correctamente.");
        return "redirect:/cursos";
    }
}
