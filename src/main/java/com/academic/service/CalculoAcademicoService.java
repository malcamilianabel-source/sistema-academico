package com.academic.service;
import com.academic.model.*;
import com.academic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.*;
import java.util.*;
@Service
public class CalculoAcademicoService {
    @Autowired private ComponenteEvaluacionRepository componentes;
    @Autowired private EvaluacionRepository calificaciones;
    /** null: todavía no existe ninguna calificación. Pendiente no equivale a cero durante el periodo activo. */
    public Double promedio(Long matriculaId,Long seccionId,boolean cierre) {
        List<ComponenteEvaluacion> lista=componentes.findByCursoPeriodoIdOrderByNombreAsc(seccionId);
        var notas=calificaciones.findByMatriculaId(matriculaId);
        double total=0,pesoConNotas=0;
        for(ComponenteEvaluacion c:lista){
            var e=notas.stream().filter(n->n.getComponente()!=null && c.getId().equals(n.getComponente().getId())).findFirst();
            if(e.isPresent()){total+=e.get().getNota()*c.getPeso();pesoConNotas+=c.getPeso();}
        }
        if(pesoConNotas==0)return cierre?0.0:null;
        double divisor=cierre?100:pesoConNotas;
        return BigDecimal.valueOf(total/divisor).setScale(2,RoundingMode.HALF_UP).doubleValue();
    }
}
