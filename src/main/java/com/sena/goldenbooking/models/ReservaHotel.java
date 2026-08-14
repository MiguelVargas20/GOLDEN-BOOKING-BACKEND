package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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

    private boolean recordatorio24hEnviado;
    
    private boolean recordatorio2hEnviado;
}