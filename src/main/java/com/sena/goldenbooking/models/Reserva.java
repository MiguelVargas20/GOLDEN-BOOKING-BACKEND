package com.sena.goldenbooking.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Document(collection = "reservas")


public class Reserva {
    @Id
    private String id;
    
    private String usuarioId; // Referencia al ID del cliente que reserva
    private LocalDateTime fechaReserva; // Cuándo se hizo la reserva
    private EstadoReserva estado;
    private TipoReserva tipo; // HOTELERA o DEPORRIVA
    
    // Estos campos son específicos pero conviven en el mismo documento NoSQL
    private ReservaHotel detalleHotel;
    private ReservaDeporte detalleDeporte;

    // Campos comunes de auditoría
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Double precioTotal;
}