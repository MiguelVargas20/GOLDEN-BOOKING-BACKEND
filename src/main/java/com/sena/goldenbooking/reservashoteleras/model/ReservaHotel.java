package com.sena.goldenbooking.reservashoteleras.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "ReservaHotel")
// Índice compuesto: findByIdHabitacionAndEstadoNot() filtra por AMBOS campos
// a la vez (es la consulta de disponibilidad, se ejecuta en cada intento de
// reserva). Un índice compuesto en el mismo orden en que se filtra es más
// eficiente que dos índices separados en idHabitacion y estado.
@CompoundIndexes({
    @CompoundIndex(name = "idHabitacion_estado_idx", def = "{'idHabitacion': 1, 'estado': 1}")
})
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ReservaHotel {

    @Id
    private String idHotelReserva;

    private String idReserva;        // referencia a Reserva padre

    private String idHabitacion;     // ← este faltaba

    // Se consulta en findByDocUsuario (endpoint /mis-reservas)
    @Indexed
    private String docUsuario;   // ← agregar este campo

    private Habitacion datosH;       // se llena en el service


    private LocalDateTime fechaCheckIn;
    private LocalDateTime fechaCheckOut;
    
    private Integer noches;
    private Double precioTotal;

    private EstadoReserva estado;

    // ── Trazabilidad del flujo de aprobación (PENDIENTE → CONFIRMADA / CANCELADA) ──
    /** true si la registró un ADMIN a nombre del cliente (ej. en recepción). */
    private boolean registradaPorAdministrador;

    /** Cuándo el cliente hizo la solicitud. */
    private LocalDateTime fechaSolicitud;
    /** Cuándo el admin la aprobó. */
    private LocalDateTime fechaConfirmacion;
    /** Cuándo se canceló, quién la canceló y por qué (el motivo es obligatorio si cancela el admin). */
    private LocalDateTime fechaCancelacion;
    private CanceladaPor canceladaPor;
    private String motivoCancelacion;

    private boolean recordatorio24hEnviado;
    
    private boolean recordatorio2hEnviado;
}