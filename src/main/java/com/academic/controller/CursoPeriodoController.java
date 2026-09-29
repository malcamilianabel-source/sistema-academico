package com.academic.controller;

import com.academic.model.CursoPeriodo;
import com.academic.service.CursoPeriodoService;
import com.academic.service.CursoService;
import com.academic.service.DocenteService;
import com.academic.service.PeriodoAcademicoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/asignaciones")
public class CursoPeriodoController {

    @Autowired private CursoPeriodoService cursoPeriodoService;
    @Autowired private CursoService cursoService;
    @Autowired private DocenteService docenteService;
    @Autowired private PeriodoAcademicoService periodoService;

    @GetMapping
    public String listar(HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) return "redirect:/login";
        model.addAttribute("asignaciones", cursoPeriodoService.listarTodos());
        model.addAttribute("cursos", cursoService.listarTodos());
        model.addAttribute("docentes", docenteService.listarTodos());
        model.addAttribute("periodos", periodoService.listarTodos());
        model.addAttribute("paginaActual", "asignaciones");
        return "asignaciones/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Long cursoId,
                          @RequestParam Long docenteId,
                          @RequestParam Long periodoId,
                          RedirectAttributes ra) {
        CursoPeriodo cp = new CursoPeriodo();
        cp.setCurso(cursoService.buscarPorId(cursoId).orElseThrow());
        cp.setDocente(docenteService.buscarPorId(docenteId).orElseThrow());
        cp.setPeriodo(periodoService.buscarPorId(periodoId).orElseThrow());
        cursoPeriodoService.guardar(cp);
        ra.addFlashAttribute("mensaje", "Curso asignado correctamente.");
        return "redirect:/asignaciones";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public CursoPeriodo editar(@PathVariable Long id) {
        return cursoPeriodoService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@RequestParam Long id,
                             @RequestParam Long cursoId,
                             @RequestParam Long docenteId,
                             @RequestParam Long periodoId,
                             RedirectAttributes ra) {
        CursoPeriodo cp = cursoPeriodoService.buscarPorId(id).orElseThrow();
        cp.setCurso(cursoService.buscarPorId(cursoId).orElseThrow());
        cp.setDocente(docenteService.buscarPorId(docenteId).orElseThrow());
        cp.setPeriodo(periodoService.buscarPorId(periodoId).orElseThrow());
        cursoPeriodoService.guardar(cp);
        ra.addFlashAttribute("mensaje", "Asignación actualizada correctamente.");
        return "redirect:/asignaciones";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        cursoPeriodoService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Asignación eliminada correctamente.");
        return "redirect:/asignaciones";
    }
}
