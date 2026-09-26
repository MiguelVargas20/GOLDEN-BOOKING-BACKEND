package com.sena.goldenbooking.cargos.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Consumo del cliente dentro del club (agua, comida, bloqueador, spa...)
 * cargado a su reserva activa o a su cuenta de socio. Se paga al hacer el
 * check-out o a fin de mes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Cargo")
@CompoundIndex(name = "doc_estado_idx", def = "{'docUsuario': 1, 'estado': 1, 'fecha': -1}")
public class Cargo {

    @Id
    private String id;

    private String docUsuario;

    private String concepto;

    private CategoriaCargo categoria;

    private int cantidad;

    private double valorUnitario;

    private double total;

    private DestinoCargo destino;

    /** Reserva de hotel o deporte (null si va a la cuenta de socio). */
    private String idReserva;

    /** "Habitación 101 · 10 oct → 12 oct" o "Cuenta de socio" (copia para mostrar). */
    private String descripcionDestino;

    private EstadoCargo estado;

    private LocalDateTime fecha;

    /** Usuario (admin) que registró el consumo. */
    private String registradoPor;

    private LocalDateTime fechaPago;

    private String pagoRegistradoPor;
}
