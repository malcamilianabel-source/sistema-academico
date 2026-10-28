package com.academic.service;

import com.academic.model.PeriodoAcademico;
import com.academic.model.Sesion;
import com.academic.repository.SesionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class SesionService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        sesion.setAula(Validaciones.limpiar(sesion.getAula()));
        Validaciones.exigir(sesion.getHoraInicio() != null || sesion.getHoraFin() == null, "horaInicio",
                "Ingresa también la hora de inicio, o deja ambas horas vacías.");
        Validaciones.exigir(sesion.getHoraFin() != null || sesion.getHoraInicio() == null, "horaFin",
                "Ingresa también la hora de fin, o deja ambas horas vacías.");
        Validaciones.exigir(sesion.getHoraInicio() == null || sesion.getHoraFin().isAfter(sesion.getHoraInicio()),
                "horaFin", "La hora de fin debe ser posterior a la hora de inicio.");

        PeriodoAcademico periodo = sesion.getCursoPeriodo() == null ? null : sesion.getCursoPeriodo().getPeriodo();
        Validaciones.exigir(periodo == null || !periodo.isCerrado(), "cursoPeriodo.id",
                "El periodo " + (periodo == null ? "" : periodo.getNombre()) + " está cerrado; no se pueden registrar sesiones.");
        LocalDate fecha = sesion.getFechaSesion();
        if (periodo != null && fecha != null && periodo.getFechaInicio() != null && periodo.getFechaFin() != null) {
            Validaciones.exigir(!fecha.isBefore(periodo.getFechaInicio()) && !fecha.isAfter(periodo.getFechaFin()),
                    "fechaSesion", "La fecha debe estar dentro del periodo " + periodo.getNombre() + " ("
                            + periodo.getFechaInicio().format(DIA) + " al " + periodo.getFechaFin().format(DIA) + ").");
        }
        return repository.save(sesion);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
