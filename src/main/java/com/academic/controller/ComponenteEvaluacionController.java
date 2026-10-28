package com.academic.controller;
import com.academic.model.*;
import com.academic.service.*;
import com.academic.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/componentes")
public class ComponenteEvaluacionController {
    @Autowired private ComponenteEvaluacionService servicio;
    @Autowired private CursoPeriodoService secciones;
    private boolean permitido(Usuario u,CursoPeriodo cp){return u!=null && ("ADMIN".equals(u.getRol()) || ("DOCENTE".equals(u.getRol()) && secciones.dictaSeccion(u.getId(),cp)));}
    @GetMapping("/{id}") public String ver(@PathVariable Long id,HttpSession ses,Model m){
        CursoPeriodo cp=secciones.buscarPorId(id).orElse(null);
        if(cp==null||!permitido((Usuario)ses.getAttribute("usuario"),cp))return "redirect:/acceso-denegado";
        m.addAttribute("seccion",cp);m.addAttribute("componentes",servicio.listar(id));m.addAttribute("paginaActual","evaluaciones");return "evaluaciones/componentes";
    }
    @PostMapping("/{id}") public String guardar(@PathVariable Long id,@RequestParam String nombre,@RequestParam Double peso,HttpSession ses,RedirectAttributes ra){
        CursoPeriodo cp=secciones.buscarPorId(id).orElse(null);
        if(cp==null||!permitido((Usuario)ses.getAttribute("usuario"),cp))return "redirect:/acceso-denegado";
        try{servicio.crear(id,nombre,peso);ra.addFlashAttribute("mensaje","Componente creado.");}
        catch(IllegalArgumentException e){ra.addFlashAttribute("error",e.getMessage());}
        return "redirect:/componentes/"+id;
    }
}
