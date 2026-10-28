package com.academic.controller;

import com.academic.model.*;
import com.academic.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller
public class MisCursosController {
    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final AsistenciaRepository asistencias;
    private final EvaluacionRepository evaluaciones;
    private final ComponenteEvaluacionRepository componentes;
    private final ContenidoSemanalRepository contenidos;
    private final AsignacionDocenteRepository docentes;

    public MisCursosController(EstudianteRepository estudiantes, MatriculaRepository matriculas,
        AsistenciaRepository asistencias, EvaluacionRepository evaluaciones,
        ComponenteEvaluacionRepository componentes, ContenidoSemanalRepository contenidos,
        AsignacionDocenteRepository docentes) {
        this.estudiantes=estudiantes; this.matriculas=matriculas;
        this.asistencias=asistencias; this.evaluaciones=evaluaciones;
        this.componentes=componentes; this.contenidos=contenidos; this.docentes=docentes;
    }

    private Estudiante estudiante(HttpSession session) {
        Usuario usuario=(Usuario)session.getAttribute("usuario");
        if(usuario==null || !"ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) return null;
        return estudiantes.findByUsuarioId(usuario.getId()).orElse(null);
    }

    @GetMapping("/mis-cursos")
    public String listar(HttpSession session, Model model) {
        if (session.getAttribute("usuario")==null) return "redirect:/login";
        Estudiante estudiante=estudiante(session);
        if(estudiante==null) return "redirect:/acceso-denegado";
        List<Matricula> lista=matriculas.findByEstudianteId(estudiante.getId()).stream()
            .filter(m->!"RETIRADO".equals(m.getEstado()))
            .sorted(Comparator.comparing((Matricula m)->m.getCursoPeriodo().getPeriodo().getFechaInicio()).reversed()
                .thenComparing(m->m.getCursoPeriodo().getCurso().getNombre()))
            .toList();
        model.addAttribute("cursos",lista);
        model.addAttribute("paginaActual","mis-cursos");
        return "mis-cursos/lista";
    }

    @GetMapping("/mis-cursos/{id}")
    public String detalle(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("usuario")==null) return "redirect:/login";
        Estudiante estudiante=estudiante(session);
        if(estudiante==null) return "redirect:/acceso-denegado";
        Matricula matricula=matriculas.findById(id).orElse(null);
        if(matricula==null || !matricula.getEstudiante().getId().equals(estudiante.getId())
            || "RETIRADO".equals(matricula.getEstado())) return "redirect:/acceso-denegado";
        Long seccionId=matricula.getCursoPeriodo().getId();
        var registros=asistencias.findByMatriculaIdOrderBySesionFechaSesionAsc(id);
        long asistio=registros.stream().filter(a->"PRESENTE".equals(a.getEstado()) || "TARDANZA".equals(a.getEstado())).count();
        var notas=evaluaciones.findByMatriculaIdOrderByFechaAsc(id);
        double suma=0, pesos=0;
        for(var nota:notas) {
            double peso=nota.getComponente()!=null && nota.getComponente().getPeso()!=null ? nota.getComponente().getPeso():1;
            if(nota.getNota()!=null) {suma+=nota.getNota()*peso;pesos+=peso;}
        }
        model.addAttribute("matricula",matricula);
        model.addAttribute("contenido",contenidos.findByCursoPeriodoIdOrderBySemanaAsc(seccionId));
        model.addAttribute("asistencias",registros);
        model.addAttribute("porcentajeAsistencia",registros.isEmpty()?null:Math.round(100.0*asistio/registros.size()));
        model.addAttribute("componentes",componentes.findByCursoPeriodoIdOrderByNombreAsc(seccionId));
        model.addAttribute("notas",notas);
        model.addAttribute("promedio",matricula.getNotaFinal()!=null?matricula.getNotaFinal():pesos>0?Math.round((suma/pesos)*100.0)/100.0:null);
        model.addAttribute("docentes",docentes.findDeSecciones(java.util.List.of(seccionId)));
        model.addAttribute("paginaActual","mis-cursos");
        return "mis-cursos/detalle";
    }
}
