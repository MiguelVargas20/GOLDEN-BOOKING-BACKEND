package com.sena.goldenbooking.cargos.dto;

import com.sena.goldenbooking.cargos.model.DestinoCargo;

/** Reserva activa o cuenta de socio a la que se le puede cargar un consumo. */
public record DestinoDisponibleDto(DestinoCargo destino, String idReserva, String descripcion) {
}
