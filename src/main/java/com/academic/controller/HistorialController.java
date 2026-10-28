package com.academic.controller;

import com.academic.dto.HistorialCurso;
import com.academic.model.Estudiante;
import com.academic.model.Usuario;
import com.academic.service.EstudianteService;
import com.academic.service.MatriculaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/historial")
public class HistorialController {

    @Autowired private EstudianteService estudianteService;
    @Autowired private MatriculaService matriculaService;

    /**
     * El estudiante ve su propio historial; el administrador elige al estudiante con ?estudianteId=.
     */
    @GetMapping
    public String ver(@RequestParam(required = false) Long estudianteId, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        Estudiante estudiante;
        if ("ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) {
            estudiante = estudianteService.buscarPorUsuario(usuario.getId()).orElse(null);
            if (estudiante == null) {
                model.addAttribute("aviso", "Tu usuario no está vinculado a ningún estudiante. "
                        + "Pide al administrador que registre un estudiante con tu mismo correo.");
            }
        } else if ("DOCENTE".equalsIgnoreCase(usuario.getRol())) {
            var permitidos = matriculaService.listarPorDocente(usuario.getId()).stream()
                    .map(Matricula -> Matricula.getEstudiante()).distinct().toList();
            model.addAttribute("estudiantes", permitidos);
            if (estudianteId != null && permitidos.stream().noneMatch(e -> estudianteId.equals(e.getId()))) {
                return "redirect:/acceso-denegado";
            }
            estudiante = estudianteId != null ? estudianteService.buscarPorId(estudianteId).orElse(null) : null;
        } else {
            model.addAttribute("estudiantes", estudianteService.listarTodos());
            estudiante = estudianteId != null ? estudianteService.buscarPorId(estudianteId).orElse(null) : null;
        }

        if (estudiante != null) {
            List<HistorialCurso> historial = matriculaService.historialPorEstudiante(estudiante.getId());
            model.addAttribute("estudiante", estudiante);
            model.addAttribute("historial", historial);
            model.addAttribute("promedioGeneral", promedioPonderado(historial));
            model.addAttribute("aprobados", historial.stream()
                    .filter(h -> "APROBADO".equals(h.getEstadoAcademico())).count());
            model.addAttribute("desaprobados", historial.stream()
                    .filter(h -> "DESAPROBADO".equals(h.getEstadoAcademico())).count());
            model.addAttribute("creditosAprobados", historial.stream()
                    .filter(h -> "APROBADO".equals(h.getEstadoAcademico()) && h.getCreditos() != null)
                    .mapToInt(HistorialCurso::getCreditos).sum());
        }
        model.addAttribute("paginaActual", "historial");
        return "historial/ver";
    }

    /** Promedio de los cursos con notas, ponderado por créditos (peso 1 si el curso no tiene créditos). */
    private Double promedioPonderado(List<HistorialCurso> historial) {
        double suma = 0;
        int pesos = 0;
        for (HistorialCurso h : historial) {
            if (!h.isConNotas() || "RETIRADO".equals(h.getEstadoMatricula())) continue;
            int peso = h.getCreditos() != null ? h.getCreditos() : 1;
            suma += h.getPromedio() * peso;
            pesos += peso;
        }
        return pesos == 0 ? null : suma / pesos;
    }
}
