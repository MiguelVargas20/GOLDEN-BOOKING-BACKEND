package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "ReservaHotel")
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ReservaHotel {

    @Id
    private String idHotelReserva;

    private String idReserva;        // referencia a Reserva padre

    private String idHabitacion;     // ← este faltaba

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