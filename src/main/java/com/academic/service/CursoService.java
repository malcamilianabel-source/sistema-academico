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
        curso.setNombre(Validaciones.limpiar(curso.getNombre()));
        curso.setCodigo(Validaciones.limpiar(curso.getCodigo()));
        String nombre = curso.getNombre();
        Validaciones.exigir(nombre != null && nombre.length() >= 3 && nombre.length() <= 150, "nombre",
                "El nombre del curso debe tener entre 3 y 150 caracteres.");
        Validaciones.exigir(Validaciones.coincide(curso.getCodigo(), "^[A-Za-z0-9\\-]{2,20}$"), "codigo",
                "Código inválido: usa de 2 a 20 letras, números o guiones.");
        Validaciones.exigir(!existeCodigo(curso.getCodigo(), curso.getId()), "codigo",
                "Este código ya está registrado en otro curso.");
        Validaciones.exigir(Validaciones.enRango(curso.getCreditos(), 1, 10), "creditos",
                "Los créditos deben estar entre 1 y 10.");
        Validaciones.exigir(Validaciones.enRango(curso.getCiclo(), 1, 10), "ciclo",
                "El ciclo debe estar entre 1 y 10.");
        Validaciones.exigir(Validaciones.enRango(curso.getHoras(), 1, 200), "horas",
                "Las horas deben estar entre 1 y 200.");
        return cursoRepository.save(curso);
    }

    /** Verifica si otro curso ya usa ese código (sin distinguir mayúsculas). */
    public boolean existeCodigo(String codigo, Long excludeId) {
        if (Validaciones.vacio(codigo)) return false;
        Optional<Curso> existente = cursoRepository.findFirstByCodigoIgnoreCase(codigo.trim());
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
