package com.universidad.reservaslabs.controller;

import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public List<Reserva> listar() {
        return reservaService.findAll();
    }

    @GetMapping("/{id}")
    public Reserva obtener(@PathVariable Long id) {
        return reservaService.findById(id);
    }

    @GetMapping("/laboratorio/{id}")
    public List<Reserva> obtenerPorLaboratorio(@PathVariable Long id) {
        return reservaService.findByLaboratorio(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reserva crear(@Valid @RequestBody Reserva reserva) {
        return reservaService.crear(reserva);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id) {
        reservaService.cancelar(id);
    }
}
