package com.academic.repository;
import com.academic.model.ContenidoSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ContenidoSemanalRepository extends JpaRepository<ContenidoSemanal,Long> {
    List<ContenidoSemanal> findByCursoPeriodoIdOrderBySemanaAsc(Long cursoPeriodoId);
    boolean existsByCursoPeriodoIdAndSemana(Long cursoPeriodoId,Integer semana);
}
