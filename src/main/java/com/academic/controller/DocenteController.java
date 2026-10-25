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

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Docente docente, BindingResult result,
                          Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("docentes", docenteService.listarTodos());
            return vistaLista(model, "modalNuevo");
        }
        docente.setTelefono(normalizarTelefono(docente.getTelefono()));

        if (docenteService.existeDni(docente.getDni(), null)) {
            ra.addFlashAttribute("error", "Ya existe un docente con el DNI " + docente.getDni() + ".");
            return "redirect:/docentes";
        }
        if (docenteService.existeEmail(docente.getEmail(), null)) {
            ra.addFlashAttribute("error", "Ya existe un docente con ese email.");
            return "redirect:/docentes";
        }
        if (docenteService.existeTelefono(docente.getTelefono(), null)) {
            ra.addFlashAttribute("error", "Ya existe un docente registrado con el teléfono " + docente.getTelefono() + ".");
            return "redirect:/docentes";
        }
        try {
            docenteService.guardar(docente);
            ra.addFlashAttribute("mensaje", "Docente guardado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/docentes";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Docente obtenerParaEditar(@PathVariable Long id) {
        return docenteService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@Valid @ModelAttribute("docenteEditar") Docente form, BindingResult result,
                             Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("docentes", docenteService.listarTodos());
            return vistaLista(model, "modalEditar");
        }
        form.setTelefono(normalizarTelefono(form.getTelefono()));

        if (docenteService.existeDni(form.getDni(), form.getId())) {
            ra.addFlashAttribute("error", "Ya existe otro docente con el DNI " + form.getDni() + ".");
            return "redirect:/docentes";
        }
        if (docenteService.existeEmail(form.getEmail(), form.getId())) {
            ra.addFlashAttribute("error", "Ya existe otro docente con ese email.");
            return "redirect:/docentes";
        }
        if (docenteService.existeTelefono(form.getTelefono(), form.getId())) {
            ra.addFlashAttribute("error", "Ya existe otro docente registrado con el teléfono " + form.getTelefono() + ".");
            return "redirect:/docentes";
        }
        Docente d = docenteService.buscarPorId(form.getId()).orElseThrow();
        d.setNombre(form.getNombre());
        d.setApellido(form.getApellido());
        d.setDni(form.getDni());
        d.setTelefono(form.getTelefono());
        d.setEspecialidad(form.getEspecialidad());
        d.setEmail(form.getEmail());
        try {
            docenteService.guardar(d);
            ra.addFlashAttribute("mensaje", "Docente actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/docentes";
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

    private String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;
        return telefono.trim();
    }
}
