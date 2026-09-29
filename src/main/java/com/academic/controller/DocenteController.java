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
    public String guardar(@ModelAttribute Docente docente,
                          RedirectAttributes redirectAttributes) {
        docenteService.guardar(docente);
        redirectAttributes.addFlashAttribute("mensaje", "Docente guardado correctamente.");
        return "redirect:/docentes";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Docente obtenerParaEditar(@PathVariable Long id) {
        return docenteService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Docente form, RedirectAttributes redirectAttributes) {
        Docente d = docenteService.buscarPorId(form.getId()).orElseThrow();
        d.setNombre(form.getNombre());
        d.setApellido(form.getApellido());
        d.setDni(form.getDni());
        d.setTelefono(form.getTelefono());
        d.setEspecialidad(form.getEspecialidad());
        d.setEmail(form.getEmail());
        docenteService.guardar(d);
        redirectAttributes.addFlashAttribute("mensaje", "Docente actualizado correctamente.");
        return "redirect:/docentes";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        docenteService.eliminar(id);
        redirectAttributes.addFlashAttribute("mensaje", "Docente eliminado correctamente.");
        return "redirect:/docentes";
    }
}
