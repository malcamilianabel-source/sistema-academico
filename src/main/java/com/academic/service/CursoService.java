package com.academic.service;

import com.academic.model.Curso;
import com.academic.repository.CursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cursoRepository;

    public List<Curso> listarTodos() {
        return cursoRepository.findAll();
    }

    public List<Curso> buscar(String termino) {
        return cursoRepository.findByNombreContainingIgnoreCase(termino);
    }

    public Optional<Curso> buscarPorId(Long id) {
        return cursoRepository.findById(id);
    }

    public Curso guardar(Curso curso) {
        String nombre = curso.getNombre();
        Validaciones.exigir(nombre != null && nombre.trim().length() >= 3 && nombre.length() <= 150,
                "El nombre del curso debe tener entre 3 y 150 caracteres.");
        Validaciones.exigir(Validaciones.coincide(curso.getCodigo(), "^[A-Za-z0-9\\-]{2,20}$"),
                "Código inválido: usa de 2 a 20 letras, números o guiones.");
        Validaciones.exigir(Validaciones.enRango(curso.getCreditos(), 1, 10), "Los créditos deben estar entre 1 y 10.");
        Validaciones.exigir(Validaciones.enRango(curso.getCiclo(), 1, 10), "El ciclo debe estar entre 1 y 10.");
        Validaciones.exigir(Validaciones.enRango(curso.getHoras(), 1, 200), "Las horas deben estar entre 1 y 200.");
        return cursoRepository.save(curso);
    }

    public boolean existeCodigo(String codigo, Long excludeId) {
        Optional<Curso> existente = cursoRepository.findByCodigo(codigo);
        if (existente.isEmpty()) return false;
        if (excludeId == null) return true;
        return !existente.get().getId().equals(excludeId);
    }

    public void eliminar(Long id) {
        cursoRepository.deleteById(id);
    }

    public long contarTotal() {
        return cursoRepository.count();
    }
}
