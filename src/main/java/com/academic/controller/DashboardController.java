package com.academic.controller;

import com.academic.model.*;
import com.academic.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;

@Controller
public class DashboardController {
    @Autowired private EstudianteService estudiantes;
    @Autowired private DocenteService docentes;
    @Autowired private CursoService cursos;
    @Autowired private UsuarioService usuarios;
    @Autowired private MatriculaService matriculas;
    @Autowired private EvaluacionService evaluaciones;
    @Autowired private CursoPeriodoService secciones;
    @Autowired private SesionService sesiones;
    @Autowired private com.academic.repository.AsistenciaRepository asistencias;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";
        model.addAttribute("paginaActual", "dashboard");
        if ("ADMIN".equals(usuario.getRol())) {
            model.addAttribute("totalEstudiantes", estudiantes.contarTotal());
            model.addAttribute("totalDocentes", docentes.contarTotal());
            model.addAttribute("totalCursos", cursos.contarTotal());
            model.addAttribute("totalUsuarios", usuarios.contarTotal());
            model.addAttribute("matriculasActivas", matriculas.contarActivas());
            model.addAttribute("estudiantesRiesgo", evaluaciones.listarEstudiantesEnRiesgo());
            return "dashboard";
        }
        if ("DOCENTE".equals(usuario.getRol())) {
            List<CursoPeriodo> propias = secciones.listarDeDocente(usuario.getId());
            model.addAttribute("misSecciones", propias);
            model.addAttribute("totalSecciones", propias.size());
            model.addAttribute("totalMatriculas", matriculas.listarPorDocente(usuario.getId()).stream()
                .filter(m -> "ACTIVO".equals(m.getEstado())).count());
            model.addAttribute("totalSesiones", sesiones.listarPorDocente(usuario.getId()).size());
            return "dashboard-docente";
        }
        Estudiante estudiante = estudiantes.buscarPorUsuario(usuario.getId()).orElse(null);
        model.addAttribute("estudiante", estudiante);
        if (estudiante != null) {
            List<Matricula> m = matriculas.buscarPorEstudiante(estudiante.getId());
            model.addAttribute("misMatriculas", m);
            model.addAttribute("totalCursos", m.stream().filter(x -> "ACTIVO".equals(x.getEstado())).count());
            List<com.academic.dto.HistorialCurso> historia = matriculas.historialPorEstudiante(estudiante.getId());
            double suma=0; int creditos=0;
            for(var h: historia) {
                if (!"RETIRADO".equals(h.getEstadoMatricula()) && h.getPromedio()!=null && h.getCreditos()!=null && h.getCreditos()>0) {
                    suma += h.getPromedio()*h.getCreditos(); creditos += h.getCreditos();
                }
            }
            model.addAttribute("promedio", creditos==0?null:suma/creditos);
        }
        return "dashboard-estudiante";
    }
}
