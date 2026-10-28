package com.academic.controller;

import com.academic.dto.SeccionForm;
import com.academic.model.CursoPeriodo;
import com.academic.model.Usuario;
import com.academic.service.CursoPeriodoService;
import com.academic.service.CursoService;
import com.academic.service.DocenteService;
import com.academic.service.PeriodoAcademicoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Secciones académicas (CursoPeriodo) y sus docentes. Se conserva la ruta /asignaciones.
 * El administrador gestiona todo; el docente solo ve sus secciones (el interceptor solo le permite GET).
 */
@Controller
@RequestMapping("/asignaciones")
public class CursoPeriodoController {

    @Autowired private CursoPeriodoService seccionService;
    @Autowired private CursoService cursoService;
    @Autowired private DocenteService docenteService;
    @Autowired private PeriodoAcademicoService periodoService;

    @GetMapping
    public String listar(@RequestParam(required = false) Long periodoId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        return vistaLista(usuario, periodoId, model, null);
    }

    // Cualquier error (sección repetida, periodo cerrado, vacantes) vuelve al modal con lo elegido
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("seccionForm") SeccionForm form, BindingResult result,
                          HttpSession session, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                CursoPeriodo seccion = seccionService.crear(form);
                ra.addFlashAttribute("mensaje", "Sección " + seccion.getSeccion() + " de "
                        + seccion.getCurso().getNombre() + " creada correctamente.");
                return "redirect:/asignaciones?periodoId=" + seccion.getPeriodo().getId();
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        return vistaLista((Usuario) session.getAttribute("usuario"), form.getPeriodoId(), model, "modalNuevo");
    }

    /** Datos para el modal de edición. */
    @GetMapping("/editar/{id}")
    @ResponseBody
    public Map<String, Object> editar(@PathVariable Long id) {
        CursoPeriodo cp = seccionService.buscarPorId(id).orElse(null);
        if (cp == null) return Map.of();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", cp.getId());
        datos.put("seccion", cp.getSeccion());
        datos.put("vacantes", cp.getVacantes());
        datos.put("etiqueta", cp.getEtiqueta());
        return datos;
    }

    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute("seccionEditar") SeccionForm form, BindingResult result,
                             HttpSession session, Model model, RedirectAttributes ra) {
        CursoPeriodo seccion = form.getId() == null ? null : seccionService.buscarPorId(form.getId()).orElse(null);
        if (seccion == null) {
            ra.addFlashAttribute("error", "La sección no existe.");
            return "redirect:/asignaciones";
        }
        if (!result.hasErrors()) {
            try {
                seccionService.actualizar(form);
                ra.addFlashAttribute("mensaje", "Sección actualizada correctamente.");
                return "redirect:/asignaciones?periodoId=" + seccion.getPeriodo().getId();
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        model.addAttribute("etiquetaEditar", seccion.getEtiqueta());
        return vistaLista((Usuario) session.getAttribute("usuario"), seccion.getPeriodo().getId(), model, "modalEditar");
    }

    @PostMapping("/{id}/docentes")
    public String agregarDocente(@PathVariable Long id, @RequestParam(required = false) Long docenteId,
                                 @RequestParam(defaultValue = "APOYO") String rol, RedirectAttributes ra) {
        try {
            seccionService.agregarDocente(id, docenteId, rol);
            ra.addFlashAttribute("mensaje", "TITULAR".equals(rol)
                    ? "Docente asignado como titular. El titular anterior quedó como apoyo."
                    : "Docente de apoyo asignado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Ese docente ya está asignado a la sección.");
        }
        return volverAlPeriodoDe(id);
    }

    @PostMapping("/{id}/docentes/{asignacionId}/quitar")
    public String quitarDocente(@PathVariable Long id, @PathVariable Long asignacionId, RedirectAttributes ra) {
        try {
            seccionService.quitarDocente(id, asignacionId);
            ra.addFlashAttribute("mensaje", "Docente quitado de la sección.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return volverAlPeriodoDe(id);
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        String volver = volverAlPeriodoDe(id);
        try {
            seccionService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Sección eliminada correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "No se puede eliminar la sección porque tiene registros relacionados.");
        }
        return volver;
    }

    private String volverAlPeriodoDe(Long seccionId) {
        return seccionService.buscarPorId(seccionId)
                .map(cp -> "redirect:/asignaciones?periodoId=" + cp.getPeriodo().getId())
                .orElse("redirect:/asignaciones");
    }

    private String vistaLista(Usuario usuario, Long periodoId, Model model, String abrirModal) {
        boolean esDocente = usuario != null && "DOCENTE".equalsIgnoreCase(usuario.getRol());
        // Sin filtro, se muestra el periodo activo (si existe)
        if (periodoId == null && !esDocente) {
            periodoId = periodoService.periodoActivo().map(p -> p.getId()).orElse(null);
        }
        List<CursoPeriodo> secciones = esDocente
                ? seccionService.listarDeDocente(usuario.getId())
                : seccionService.listar(periodoId);

        model.addAttribute("asignaciones", secciones);
        model.addAttribute("docentesPorSeccion", seccionService.docentesPorSeccion(secciones));
        model.addAttribute("matriculados", seccionService.matriculadosPorSeccion());
        model.addAttribute("periodos", periodoService.listarTodos());
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("cursos", cursoService.listarTodos());
        model.addAttribute("docentes", docenteService.listarTodos());
        if (!model.containsAttribute("seccionForm")) {
            SeccionForm nuevo = new SeccionForm();
            nuevo.setPeriodoId(periodoId);
            model.addAttribute("seccionForm", nuevo);
        }
        if (!model.containsAttribute("seccionEditar")) model.addAttribute("seccionEditar", new SeccionForm());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "asignaciones");
        return "asignaciones/lista";
    }
}
