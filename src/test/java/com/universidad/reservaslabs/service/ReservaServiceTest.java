package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.exception.ReservaInvalidaException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Laboratorio laboratorio;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        laboratorio = new Laboratorio(1L, "Lab 1", "Edificio A", 30, "Computación");
        reserva = new Reserva();
        reserva.setLaboratorio(laboratorio);
        reserva.setInicio(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        reserva.setFin(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0));
    }

    @Test
    void crearReservaExitosa() {
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorio));
        when(reservaRepository.buscarSolapamientos(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> {
            Reserva r = i.getArgument(0);
            r.setId(100L);
            return r;
        });

        Reserva creada = reservaService.crear(reserva);

        assertNotNull(creada.getId());
        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void crearReservaLaboratorioInexistente() {
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.crear(reserva));
    }

    @Test
    void crearReservaDuracionInvalidaCorta() {
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorio));
        reserva.setFin(reserva.getInicio().plusMinutes(20)); // menos de 30 min

        assertThrows(ReservaInvalidaException.class, () -> reservaService.crear(reserva));
    }

    @Test
    void crearReservaDuracionInvalidaLarga() {
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorio));
        reserva.setFin(reserva.getInicio().plusHours(4)); // más de 3 horas

        assertThrows(ReservaInvalidaException.class, () -> reservaService.crear(reserva));
    }

    @Test
    void crearReservaHorarioInvalidoNoche() {
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorio));
        reserva.setInicio(LocalDateTime.now().plusDays(1).withHour(22).withMinute(0));
        reserva.setFin(LocalDateTime.now().plusDays(1).withHour(23).withMinute(0));

        assertThrows(ReservaInvalidaException.class, () -> reservaService.crear(reserva));
    }

    @Test
    void crearReservaSolapamiento() {
        when(laboratorioRepository.findById(1L)).thenReturn(Optional.of(laboratorio));
        when(reservaRepository.buscarSolapamientos(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Reserva())); // Simula solapamiento

        assertThrows(ReservaConflictException.class, () -> reservaService.crear(reserva));
    }

    @Test
    void cancelarReservaExitosa() {
        reserva.setId(1L);
        // Inicio en el futuro
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        reservaService.cancelar(1L);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void cancelarReservaTardia() {
        reserva.setId(1L);
        reserva.setInicio(LocalDateTime.now().minusHours(1)); // ya pasó
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ReservaConflictException.class, () -> reservaService.cancelar(1L));
    }
}
