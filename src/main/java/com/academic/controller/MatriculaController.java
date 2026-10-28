package com.academic.controller;

import com.academic.dto.MatriculaForm;
import com.academic.model.CursoPeriodo;
import com.academic.model.Estudiante;
import com.academic.model.Matricula;
import com.academic.model.Usuario;
import com.academic.service.CursoPeriodoService;
import com.academic.service.CursoService;
import com.academic.service.EstudianteService;
import com.academic.service.MatriculaService;
import com.academic.service.PeriodoAcademicoService;
import jakarta.servlet.http.HttpServletRequest;
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
 * Matrículas. El estudiante ve su matrícula y se inscribe (matrícula múltiple);
 * el administrador busca con filtros, ve el detalle, registra, retira y reactiva.
 * El interceptor solo deja al estudiante GET /matriculas y POST /matriculas/registrar; el docente no entra.
 */
@Controller
@RequestMapping("/matriculas")
public class MatriculaController {

    @Autowired private MatriculaService matriculaService;
    @Autowired private EstudianteService estudianteService;
    @Autowired private CursoPeriodoService cursoPeriodoService;
    @Autowired private CursoService cursoService;
    @Autowired private PeriodoAcademicoService periodoService;

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String periodoId,
                         @RequestParam(required = false) String cursoId,
                         @RequestParam(required = false) String seccionId,
                         @RequestParam(required = false) String estado,
                         HttpServletRequest request, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("paginaActual", "matriculas");
        model.addAttribute("periodoActual", matriculaService.getPeriodoMatricula());
        if ("ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) {
            return vistaEstudiante(usuario, model);
        }

        // Sin ningún parámetro (primera visita) se muestra el periodo activo; "Todos" envía el campo vacío
        Long periodo = request.getQueryString() == null
                ? periodoService.periodoActivo().map(p -> p.getId()).orElse(null)
                : numero(periodoId);
        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("q", q == null ? "" : q.trim());
        filtros.put("periodoId", periodo);
        filtros.put("cursoId", numero(cursoId));
        filtros.put("seccionId", numero(seccionId));
        filtros.put("estado", estado == null ? "" : estado);
        model.addAttribute("filtros", filtros);
        model.addAttribute("consulta", request.getQueryString() == null ? "" : request.getQueryString());
        return vistaAdmin(model, null);
    }

    private String vistaEstudiante(Usuario usuario, Model model) {
        Estudiante estudiante = estudianteService.buscarPorUsuario(usuario.getId()).orElse(null);
        if (estudiante == null) {
            model.addAttribute("aviso", "Tu usuario no está vinculado a un estudiante. Contacta al administrador.");
        } else {
            List<CursoPeriodo> secciones = matriculaService.cursosDisponibles();
            model.addAttribute("cursosPeriodo", secciones);
            model.addAttribute("matriculados", cursoPeriodoService.matriculadosPorSeccion());
            model.addAttribute("docentesPorSeccion", cursoPeriodoService.docentesPorSeccion(secciones));
            model.addAttribute("matriculas", matriculaService.buscarPorEstudiante(estudiante.getId()));
        }
        return "matriculas/estudiante";
    }

    @SuppressWarnings("unchecked")
    private String vistaAdmin(Model model, String abrirModal) {
        if (!model.containsAttribute("filtros")) {
            Map<String, Object> filtros = new LinkedHashMap<>();
            filtros.put("q", "");
            filtros.put("periodoId", periodoService.periodoActivo().map(p -> p.getId()).orElse(null));
            filtros.put("cursoId", null);
            filtros.put("seccionId", null);
            filtros.put("estado", "");
            model.addAttribute("filtros", filtros);
            model.addAttribute("consulta", "");
        }
        Map<String, Object> f = (Map<String, Object>) model.getAttribute("filtros");
        List<Matricula> matriculas = matriculaService.filtrar((String) f.get("q"), (Long) f.get("periodoId"),
                (Long) f.get("cursoId"), (Long) f.get("seccionId"), (String) f.get("estado"));

        Map<String, Long> totales = new LinkedHashMap<>();
        for (String e : MatriculaService.ESTADOS) {
            totales.put(e, matriculas.stream().filter(m -> e.equals(m.getEstado())).count());
        }
        model.addAttribute("matriculas", matriculas);
        model.addAttribute("totales", totales);
        model.addAttribute("estados", MatriculaService.ESTADOS);
        model.addAttribute("periodos", periodoService.listarTodos());
        model.addAttribute("cursos", cursoService.listarTodos());
        model.addAttribute("secciones", cursoPeriodoService.listarTodos());
        model.addAttribute("seccionesMatricula", matriculaService.seccionesParaMatricular());
        model.addAttribute("matriculados", cursoPeriodoService.matriculadosPorSeccion());
        model.addAttribute("estudiantes", estudianteService.listarOrdenados());
        if (!model.containsAttribute("matriculaForm")) model.addAttribute("matriculaForm", new MatriculaForm());
        model.addAttribute("abrirModal", abrirModal);
        model.addAttribute("paginaActual", "matriculas");
        return "matriculas/lista";
    }

    /** Matrícula múltiple del propio estudiante. */
    @PostMapping("/registrar")
    public String registrar(@RequestParam(value = "cursosIds", required = false) List<Long> cursosIds,
                            HttpSession session, RedirectAttributes ra) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!"ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) return "redirect:/acceso-denegado";
        try {
            matriculaService.matricularCursos(usuario.getId(), cursosIds);
            ra.addFlashAttribute("mensaje", "Los cursos se registraron correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "No se pudo completar la matrícula. Comprueba los cursos seleccionados.");
        }
        return "redirect:/matriculas";
    }

    /** Matrícula registrada por el administrador. Los errores vuelven al modal con lo elegido. */
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("matriculaForm") MatriculaForm form, BindingResult result,
                          HttpSession session, Model model, RedirectAttributes ra) {
        if (!esAdmin(session)) return "redirect:/acceso-denegado";
        if (!result.hasErrors()) {
            try {
                Matricula m = matriculaService.registrarPorAdmin(form);
                CursoPeriodo cp = m.getCursoPeriodo();
                ra.addFlashAttribute("mensaje", "Se matriculó a " + m.getEstudiante().getNombre() + " "
                        + m.getEstudiante().getApellido() + " en " + cp.getCurso().getNombre()
                        + " (sección " + cp.getSeccion() + ", " + cp.getPeriodo().getNombre() + ").");
                return "redirect:/matriculas?periodoId=" + cp.getPeriodo().getId() + "&seccionId=" + cp.getId();
            } catch (IllegalArgumentException e) {
                Formularios.rechazar(result, e);
            } catch (DataIntegrityViolationException e) {
                Formularios.rechazarRegistroSimultaneo(result);
            }
        }
        return vistaAdmin(model, "modalNueva");
    }

    @GetMapping("/{id:\\d+}")
    public String detalle(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        if (!esAdmin(session)) return "redirect:/acceso-denegado";
        if (matriculaService.buscarPorId(id).isEmpty()) {
            ra.addFlashAttribute("error", "La matrícula no existe.");
            return "redirect:/matriculas";
        }
        var detalle = matriculaService.detalle(id);
        CursoPeriodo cp = detalle.matricula().getCursoPeriodo();
        model.addAttribute("d", detalle);
        model.addAttribute("docentes", cursoPeriodoService.docentesPorSeccion(List.of(cp)).get(cp.getId()));
        model.addAttribute("inscritos", cursoPeriodoService.matriculadosPorSeccion().getOrDefault(cp.getId(), 0L));
        model.addAttribute("paginaActual", "matriculas");
        return "matriculas/detalle";
    }

    /** ACTIVO → RETIRADO. */
    @PostMapping("/baja/{id}")
    public String baja(@PathVariable Long id, @RequestParam(required = false) String volver,
                       HttpSession session, RedirectAttributes ra) {
        if (!esAdmin(session)) return "redirect:/acceso-denegado";
        try {
            matriculaService.darDeBaja(id);
            ra.addFlashAttribute("mensaje", "Matrícula retirada. Se conservan sus notas y asistencias.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return volverA(id, volver);
    }

    /** RETIRADO → ACTIVO, para corregir un retiro hecho por error. */
    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, @RequestParam(required = false) String volver,
                            HttpSession session, RedirectAttributes ra) {
        if (!esAdmin(session)) return "redirect:/acceso-denegado";
        try {
            matriculaService.reactivar(id);
            ra.addFlashAttribute("mensaje", "Matrícula reactivada.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return volverA(id, volver);
    }

    /** Vuelve al detalle o a la lista con los mismos filtros (solo se acepta una consulta simple). */
    private String volverA(Long id, String volver) {
        if ("detalle".equals(volver)) return "redirect:/matriculas/" + id;
        if (volver != null && !volver.isEmpty() && volver.matches("[A-Za-z0-9=&%._+\\-]+")) {
            return "redirect:/matriculas?" + volver;
        }
        return "redirect:/matriculas";
    }

    private boolean esAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && "ADMIN".equalsIgnoreCase(usuario.getRol());
    }

    private static Long numero(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
