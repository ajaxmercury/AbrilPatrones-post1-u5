package com.universidad.reservaslabs.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El laboratorio es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "laboratorio_id")
    private Laboratorio laboratorio;

    @NotBlank(message = "El nombre del solicitante es obligatorio")
    private String nombreSolicitante;

    @Email(message = "El correo del solicitante debe ser válido")
    private String correoSolicitante;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fin;

    private String motivo;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;
}
