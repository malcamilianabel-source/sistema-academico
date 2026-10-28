package com.academic.controller;

import com.academic.model.Docente;
import com.academic.service.DocenteService;
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
@RequestMapping("/docentes")
public class DocenteController {

    @Autowired
    private DocenteService docenteService;

    @GetMapping
    public String listar(@RequestParam(required = false) String buscar,
                         HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/login";

        List<Docente> lista;
        if (buscar != null && !buscar.isBlank()) {
            lista = docenteService.buscar(buscar);
            model.addAttribute("buscar", buscar);
        } else {
            lista = docenteService.listarTodos();
        }

        model.addAttribute("docentes", lista);
        return vistaLista(model, null);
    }

    // Cualquier error (formato o dato repetido) vuelve al modal con lo escrito y el campo marcado.
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Docente docente, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                docenteService.registrar(docente);
                ra.addFlashAttribute("mensaje", "Docente guardado correctamente.");
                return "redirect:/docentes";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        model.addAttribute("docentes", docenteService.listarTodos());
        return vistaLista(model, "modalNuevo");
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Docente obtenerParaEditar(@PathVariable Long id) {
        return docenteService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("docenteEditar") Docente form, BindingResult result,
                             Model model, RedirectAttributes ra) {
        if (form.getId() == null || docenteService.buscarPorId(form.getId()).isEmpty()) {
            ra.addFlashAttribute("error", "El docente no existe.");
            return "redirect:/docentes";
        }
        if (!result.hasErrors()) {
            try {
                docenteService.actualizar(form);
                ra.addFlashAttribute("mensaje", "Docente actualizado correctamente.");
                return "redirect:/docentes";
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        model.addAttribute("docentes", docenteService.listarTodos());
        return vistaLista(model, "modalEditar");
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            docenteService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Docente eliminado correctamente.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar el docente porque tiene cursos asignados.");
        }
        return "redirect:/docentes";
    }

    /** Completa el modelo de la lista; si viene de un formulario con errores, reabre su modal. */
    private String vistaLista(Model model, String abrirModal) {
        if (!model.containsAttribute("docente")) model.addAttribute("docente", new Docente());
        if (!model.containsAttribute("docenteEditar")) model.addAttribute("docenteEditar", new Docente());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "docentes");
        return "docentes/lista";
    }
}
