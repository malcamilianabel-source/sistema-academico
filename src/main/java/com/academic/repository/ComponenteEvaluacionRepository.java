package com.academic.repository;
import com.academic.model.ComponenteEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ComponenteEvaluacionRepository extends JpaRepository<ComponenteEvaluacion,Long> {
    List<ComponenteEvaluacion> findByCursoPeriodoIdOrderByNombreAsc(Long cursoPeriodoId);
    boolean existsByCursoPeriodoIdAndNombreIgnoreCase(Long cursoPeriodoId,String nombre);
}
