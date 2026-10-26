package com.academic.service;

import com.academic.model.Sesion;
import com.academic.repository.SesionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SesionService {

    @Autowired
    private SesionRepository repository;

    public List<Sesion> listarTodos() {
        return repository.findAllOrdenadas();
    }

    public List<Sesion> listarPorDocente(Long usuarioId) {
        return repository.findByDocenteUsuarioId(usuarioId);
    }

    public Optional<Sesion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Sesion guardar(Sesion sesion) {
        Validaciones.exigir(sesion.getHoraInicio() == null || sesion.getHoraFin() == null
                        || sesion.getHoraFin().isAfter(sesion.getHoraInicio()),
                "La hora de fin debe ser posterior a la hora de inicio.");
        return repository.save(sesion);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
