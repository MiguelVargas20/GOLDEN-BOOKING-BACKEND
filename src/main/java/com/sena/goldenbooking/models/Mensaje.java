package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "Mensaje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    private String id;

    private String nombre;
    private String correo;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private boolean leido;

    private String respuesta;             // texto que el admin envía como respuesta
    private LocalDateTime fechaRespuesta;  // cuándo se respondió (null si no se ha respondido)
    private boolean respuestaVista;        // si el usuario dueño del mensaje ya vio la respuesta (para el badge de notificaciones)
}