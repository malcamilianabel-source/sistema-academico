package com.academic.controller;

import com.academic.model.*;
import com.academic.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/contenido-curso")
public class ContenidoCursoController {
    private final CursoPeriodoRepository secciones;
    private final ContenidoSemanalRepository contenidos;
    private final AsignacionDocenteRepository asignaciones;

    public ContenidoCursoController(CursoPeriodoRepository secciones, ContenidoSemanalRepository contenidos,
                                   AsignacionDocenteRepository asignaciones) {
        this.secciones=secciones;this.contenidos=contenidos;this.asignaciones=asignaciones;
    }

    private boolean autorizado(Usuario u,CursoPeriodo seccion) {
        if(u==null || seccion==null) return false;
        return "ADMIN".equalsIgnoreCase(u.getRol()) || ("DOCENTE".equalsIgnoreCase(u.getRol()) &&
            (seccion.getDocente()!=null && seccion.getDocente().getUsuario()!=null &&
                u.getId().equals(seccion.getDocente().getUsuario().getId()) ||
                asignaciones.existsByCursoPeriodoIdAndDocenteUsuarioId(seccion.getId(),u.getId())));
    }
    @GetMapping("/{seccionId}")
    public String lista(@PathVariable Long seccionId,HttpSession session,Model model) {
        CursoPeriodo cp=secciones.findById(seccionId).orElse(null);
        if(!autorizado((Usuario)session.getAttribute("usuario"),cp)) return "redirect:/acceso-denegado";
        model.addAttribute("seccion",cp);
        model.addAttribute("semanas",contenidos.findByCursoPeriodoIdOrderBySemanaAsc(seccionId));
        model.addAttribute("paginaActual","cursos");
        return "mis-cursos/editar-contenido";
    }
    @PostMapping("/{seccionId}/guardar")
    public String guardar(@PathVariable Long seccionId, @RequestParam Integer semana,
                          @RequestParam String titulo,@RequestParam(required=false) String descripcion,
                          @RequestParam(required=false) String enlaceMaterial,
                          HttpSession session,RedirectAttributes ra) {
        CursoPeriodo cp=secciones.findById(seccionId).orElse(null);
        if(!autorizado((Usuario)session.getAttribute("usuario"),cp)) return "redirect:/acceso-denegado";
        if(semana==null || semana<1 || semana>30 || titulo==null || titulo.isBlank() || titulo.length()>150 ||
           (descripcion!=null && descripcion.length()>1500) || (enlaceMaterial!=null && enlaceMaterial.length()>500) ||
           (enlaceMaterial!=null && !enlaceMaterial.isBlank() && !enlaceMaterial.matches("https?://.+"))) {
           ra.addFlashAttribute("error","Verifica la semana, el título y el enlace HTTPS del material.");
           return "redirect:/contenido-curso/"+seccionId;
        }
        ContenidoSemanal contenido=contenidos.findByCursoPeriodoIdOrderBySemanaAsc(seccionId).stream()
            .filter(c->semana.equals(c.getSemana())).findFirst().orElseGet(ContenidoSemanal::new);
        contenido.setCursoPeriodo(cp);contenido.setSemana(semana);contenido.setTitulo(titulo.trim());
        contenido.setDescripcion(descripcion);contenido.setEnlaceMaterial(enlaceMaterial);
        contenidos.save(contenido);
        ra.addFlashAttribute("mensaje","Semana guardada correctamente.");
        return "redirect:/contenido-curso/"+seccionId;
    }
    @PostMapping("/{seccionId}/eliminar/{id}")
    public String eliminar(@PathVariable Long seccionId,@PathVariable Long id,HttpSession session,RedirectAttributes ra) {
        CursoPeriodo cp=secciones.findById(seccionId).orElse(null);
        if(!autorizado((Usuario)session.getAttribute("usuario"),cp)) return "redirect:/acceso-denegado";
        contenidos.findById(id).filter(c->c.getCursoPeriodo().getId().equals(seccionId)).ifPresent(contenidos::delete);
        ra.addFlashAttribute("mensaje","Contenido eliminado.");
        return "redirect:/contenido-curso/"+seccionId;
    }
}
