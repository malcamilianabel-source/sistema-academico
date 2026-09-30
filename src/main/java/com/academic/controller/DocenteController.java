package com.academic.controller;

import com.academic.model.Docente;
import com.academic.service.DocenteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
        model.addAttribute("nuevoDocente", new Docente());
        model.addAttribute("paginaActual", "docentes");
        return "docentes/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Docente docente, RedirectAttributes ra) {
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
        docenteService.guardar(docente);
        ra.addFlashAttribute("mensaje", "Docente guardado correctamente.");
        return "redirect:/docentes";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Docente obtenerParaEditar(@PathVariable Long id) {
        return docenteService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Docente form, RedirectAttributes ra) {
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
        docenteService.guardar(d);
        ra.addFlashAttribute("mensaje", "Docente actualizado correctamente.");
        return "redirect:/docentes";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        docenteService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Docente eliminado correctamente.");
        return "redirect:/docentes";
    }

    private String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;
        return telefono.trim();
    }
}
