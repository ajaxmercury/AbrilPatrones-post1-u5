package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.exception.ReservaInvalidaException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final LaboratorioRepository laboratorioRepository;

    public ReservaService(ReservaRepository reservaRepository, LaboratorioRepository laboratorioRepository) {
        this.reservaRepository = reservaRepository;
        this.laboratorioRepository = laboratorioRepository;
    }

    public List<Reserva> findAll() {
        return reservaRepository.findAll();
    }

    public Reserva findById(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la reserva con ID: " + id));
    }

    public List<Reserva> findByLaboratorio(Long laboratorioId) {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw new RecursoNoEncontradoException("No se encontró el laboratorio con ID: " + laboratorioId);
        }
        return reservaRepository.findByLaboratorioId(laboratorioId);
    }

    public Reserva crear(Reserva reserva) {
        if (reserva.getLaboratorio() == null || reserva.getLaboratorio().getId() == null) {
            throw new ReservaInvalidaException("El laboratorio es obligatorio");
        }
        
        Laboratorio laboratorio = laboratorioRepository.findById(reserva.getLaboratorio().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el laboratorio con ID: " + reserva.getLaboratorio().getId()));

        validarHorarioYDuracion(reserva.getInicio(), reserva.getFin());

        List<Reserva> solapamientos = reservaRepository.buscarSolapamientos(laboratorio.getId(), reserva.getInicio(), reserva.getFin());
        if (!solapamientos.isEmpty()) {
            throw new ReservaConflictException("El laboratorio " + laboratorio.getNombre() + " ya tiene una reserva en ese horario");
        }

        reserva.setLaboratorio(laboratorio);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reservaRepository.save(reserva);
    }

    private void validarHorarioYDuracion(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            throw new ReservaInvalidaException("Las fechas de inicio y fin son obligatorias");
        }
        
        if (!inicio.isBefore(fin)) {
            throw new ReservaInvalidaException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        long minutos = Duration.between(inicio, fin).toMinutes();
        if (minutos < 30 || minutos > 180) {
            throw new ReservaInvalidaException("La duración de la reserva debe ser entre 30 minutos y 3 horas");
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();
        LocalTime apertura = LocalTime.of(7, 0);
        LocalTime cierre = LocalTime.of(21, 0);

        if (horaInicio.isBefore(apertura) || horaFin.isAfter(cierre)) {
            throw new ReservaInvalidaException("El horario de reserva debe estar dentro del horario de atención (07:00 a 21:00)");
        }
    }

    public void cancelar(Long id) {
        Reserva reserva = findById(id);
        
        if (reserva.getInicio().isBefore(LocalDateTime.now())) {
            throw new ReservaConflictException("No se puede cancelar una reserva cuyo horario de inicio ya ha pasado");
        }
        
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
    }
}
