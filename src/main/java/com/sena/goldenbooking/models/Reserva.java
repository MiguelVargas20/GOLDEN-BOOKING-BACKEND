package com.sena.goldenbooking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "Reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    private String id;

    private String docUsuario;      // Documento del cliente que reserva

    private LocalDateTime fechaR;   // Cuándo se hizo la reserva

    private EstadoReserva estR;     // PENDIENTE, CONFIRMADA, CANCELADA

    private TipoReserva tipR;       // HOTELERA, DEPORTIVA

    // Solo uno de estos dos tendrá valor según tipR
    private ReservaHotel detalleH;
    private ReservaDeporte detalleD;

    private LocalDateTime fechaI;   // Fecha inicio
    private LocalDateTime fechaF;   // Fecha fin
    private Double precioT;         // Precio total
}