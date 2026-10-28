package com.academic.controller;

import com.academic.model.Curso;
import com.academic.service.CursoService;
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
        return vistaLista(model, null);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Curso curso, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        // Cualquier error (formato o código repetido) vuelve al modal con lo escrito y el campo marcado
        if (!result.hasErrors()) {
            try {
                curso.setId(null);
                cursoService.guardar(curso);
                redirectAttributes.addFlashAttribute("mensaje", "Curso guardado correctamente.");
                return "redirect:/cursos";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        model.addAttribute("cursos", cursoService.listarTodos());
        return vistaLista(model, "modalNuevo");
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Curso obtenerParaEditar(@PathVariable Long id) {
        return cursoService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("cursoEditar") Curso curso, BindingResult result,
                             Model model, RedirectAttributes redirectAttributes) {
        if (curso.getId() == null || cursoService.buscarPorId(curso.getId()).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El curso no existe.");
            return "redirect:/cursos";
        }
        if (!result.hasErrors()) {
            try {
                cursoService.guardar(curso);
                redirectAttributes.addFlashAttribute("mensaje", "Curso actualizado correctamente.");
                return "redirect:/cursos";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        model.addAttribute("cursos", cursoService.listarTodos());
        return vistaLista(model, "modalEditar");
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        try {
            cursoService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Curso eliminado correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede eliminar el curso porque tiene asignaciones registradas.");
        }
        return "redirect:/cursos";
    }

    /** Completa el modelo de la lista; si viene de un formulario con errores, reabre su modal. */
    private String vistaLista(Model model, String abrirModal) {
        if (!model.containsAttribute("curso")) model.addAttribute("curso", new Curso());
        if (!model.containsAttribute("cursoEditar")) model.addAttribute("cursoEditar", new Curso());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "cursos");
        return "cursos/lista";
    }
}
