package com.sena.goldenbooking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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

    // Se consulta en findByDocumentoUsuario y findByDocumentoUsuarioAndTipo
    @Indexed
    private String documentoUsuario;

    private TipoReserva tipo;           // HOTEL o DEPORTE

    // Se consulta en findByEstado
    @Indexed
    private EstadoReserva estado;       // PENDIENTE, CONFIRMADA, CANCELADA

    private LocalDateTime fechaReserva; // cuándo se creó

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;
    
    private Double precioTotal;       // Precio total   
}