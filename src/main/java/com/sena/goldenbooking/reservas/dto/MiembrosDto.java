package com.sena.goldenbooking.reservas.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.sena.goldenbooking.reservas.model.MiembroReserva;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lista completa de acompañantes de una reserva (reemplaza la anterior). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Acompañantes de la reserva.")
public class MiembrosDto {

    @NotNull(message = "Envía la lista de acompañantes (puede ir vacía).")
    @Valid
    @Size(max = 50, message = "Máximo 50 acompañantes.")
    private List<MiembroReserva> miembros;
}
