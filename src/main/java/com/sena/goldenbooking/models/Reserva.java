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
    
    private String userId; // Referencia al ID del cliente que reserva
    private LocalDateTime fechaR; // Cuándo se hizo la reserva
    private EstadoReserva est;
    private TipoReserva tip; // HOTELERA o DEPORRIVA
    
    // Estos campos son específicos pero conviven en el mismo documento NoSQL
    private ReservaHotel detalleH;
    private ReservaDeporte detalleD;

    // Campos comunes de auditoría
    private LocalDateTime fechaI;
    private LocalDateTime fechaF;
    private Double precioT;
}