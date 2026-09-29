package com.academic.controller;

import com.academic.model.Evaluacion;
import com.academic.service.EvaluacionService;
import com.academic.service.MatriculaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/evaluaciones")
public class EvaluacionController {

    @Autowired
    private EvaluacionService evaluacionService;

    @Autowired
    private MatriculaService matriculaService;

    @GetMapping
    public String listar(HttpSession session, Model model) {
        if (session.getAttribute("usuario") == null) {
            return "redirect:/login";
        }

        model.addAttribute("evaluaciones", evaluacionService.listarTodos());
        model.addAttribute("matriculas", matriculaService.listarTodos());
        model.addAttribute("paginaActual", "evaluaciones");
        return "evaluaciones/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Long matriculaId,
                          @RequestParam String tipoEvaluacion,
                          @RequestParam Double nota,
                          @RequestParam(required = false) LocalDate fecha,
                          RedirectAttributes ra) {
        try {
            Evaluacion evaluacion = new Evaluacion();
            evaluacion.setMatricula(matriculaService.buscarPorId(matriculaId).orElseThrow());
            evaluacion.setTipoEvaluacion(tipoEvaluacion);
            evaluacion.setNota(nota);
            evaluacion.setFecha(fecha != null ? fecha : LocalDate.now());
            evaluacionService.guardar(evaluacion);
            ra.addFlashAttribute("mensaje", "Calificación registrada correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/evaluaciones";
    }

    @GetMapping("/editar/{id}")
    @ResponseBody
    public Evaluacion obtenerParaEditar(@PathVariable Long id) {
        return evaluacionService.buscarPorId(id).orElse(null);
    }

    @PostMapping("/actualizar")
    public String actualizar(@RequestParam Long id,
                             @RequestParam String tipoEvaluacion,
                             @RequestParam Double nota,
                             @RequestParam(required = false) LocalDate fecha,
                             RedirectAttributes ra) {
        try {
            Evaluacion evaluacion = evaluacionService.buscarPorId(id).orElseThrow();
            evaluacion.setTipoEvaluacion(tipoEvaluacion);
            evaluacion.setNota(nota);
            evaluacion.setFecha(fecha);
            evaluacionService.guardar(evaluacion);
            ra.addFlashAttribute("mensaje", "Calificación actualizada correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/evaluaciones";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        evaluacionService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Calificación eliminada correctamente.");
        return "redirect:/evaluaciones";
    }
}
