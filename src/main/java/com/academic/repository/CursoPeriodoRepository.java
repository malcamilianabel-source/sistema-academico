package com.academic.repository;

import com.academic.model.CursoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CursoPeriodoRepository extends JpaRepository<CursoPeriodo, Long> {
}
