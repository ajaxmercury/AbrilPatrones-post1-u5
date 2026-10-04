package com.universidad.reservaslabs.controller;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    private final LaboratorioRepository laboratorioRepository;

    public LaboratorioController(LaboratorioRepository laboratorioRepository) {
        this.laboratorioRepository = laboratorioRepository;
    }

    @GetMapping
    public List<Laboratorio> listar() {
        return laboratorioRepository.findAll();
    }

    @GetMapping("/{id}")
    public Laboratorio obtener(@PathVariable Long id) {
        return laboratorioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el laboratorio con ID: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Laboratorio crear(@Valid @RequestBody Laboratorio laboratorio) {
        return laboratorioRepository.save(laboratorio);
    }
}
