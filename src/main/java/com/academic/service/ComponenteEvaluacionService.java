package com.academic.service;

import com.academic.model.*;
import com.academic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ComponenteEvaluacionService {
    @Autowired private ComponenteEvaluacionRepository repository;
    @Autowired private CursoPeriodoRepository secciones;
    @Autowired private EvaluacionRepository notas;
    public List<ComponenteEvaluacion> todos() {return repository.findAll();}
    public List<ComponenteEvaluacion> listar(Long seccionId) {return repository.findByCursoPeriodoIdOrderByNombreAsc(seccionId);}
    public ComponenteEvaluacion buscar(Long id) {return repository.findById(id).orElseThrow(()->new IllegalArgumentException("Componente no encontrado."));}
    @Transactional
    public ComponenteEvaluacion crear(Long seccionId,String nombre,Double peso) {
        CursoPeriodo cp=secciones.findById(seccionId).orElseThrow(()->new IllegalArgumentException("Sección inexistente."));
        if(cp.getPeriodo().isCerrado())throw new IllegalArgumentException("No se puede modificar un periodo cerrado.");
        if(nombre==null || nombre.isBlank() || nombre.trim().length()>50)throw new IllegalArgumentException("Nombre de evaluación inválido.");
        if(peso==null || !Double.isFinite(peso) || peso<=0 || peso>100)throw new IllegalArgumentException("Peso inválido (0-100).");
        if(repository.existsByCursoPeriodoIdAndNombreIgnoreCase(seccionId,nombre.trim()))throw new IllegalArgumentException("Esta evaluación ya existe en la sección.");
        double suma=repository.findByCursoPeriodoIdOrderByNombreAsc(seccionId).stream().mapToDouble(ComponenteEvaluacion::getPeso).sum();
        if(suma+peso>100.00001)throw new IllegalArgumentException("Los pesos superan el 100 %.");
        ComponenteEvaluacion c=new ComponenteEvaluacion(); c.setCursoPeriodo(cp);c.setNombre(nombre.trim());c.setPeso(peso);
        return repository.save(c);
    }
}
