package com.sena.goldenbooking.compartido.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

import com.sena.goldenbooking.reservasdeportivas.dto.ReservaDeporteDto;
import com.sena.goldenbooking.reservashoteleras.dto.ReservaHotelDto;

import tools.jackson.databind.json.JsonMapper;

/**
 * El JSON que envía el front al reservar debe poder leerse con el mapper
 * real de la aplicación (Jackson 3). Antes fallaba con "formato inválido"
 * porque al JSON le faltan booleanos que decide el servidor.
 */
@JsonTest
@ContextConfiguration(classes = JsonReservasTest.Config.class)
class JsonReservasTest {

    @EnableAutoConfiguration
    static class Config {
    }

    @Autowired
    private JsonMapper mapper;

    @Test
    void leeLaReservaDeportivaQueEnviaElFront() {
        ReservaDeporteDto dto = mapper.readValue("""
                {"espacioId":"e1","docUsuario":"1","fInicioReserva":"2026-09-27T10:00:00",
                 "fFinReserva":"2026-09-27T11:00:00","implAlquilados":"","rqrEntrenador":false}
                """, ReservaDeporteDto.class);
        assertEquals(LocalDateTime.of(2026, 9, 27, 10, 0), dto.getFInicioReserva());
    }

    @Test
    void leeLaReservaHoteleraQueEnviaElFront() {
        ReservaHotelDto dto = mapper.readValue("""
                {"idHabitacion":"h1","docUsuario":"1","fCheckIn":"2026-09-27T15:00:00","fCheckOut":"2026-09-29T12:00:00"}
                """, ReservaHotelDto.class);
        assertEquals(LocalDateTime.of(2026, 9, 27, 15, 0), dto.getFCheckIn());
    }
}
