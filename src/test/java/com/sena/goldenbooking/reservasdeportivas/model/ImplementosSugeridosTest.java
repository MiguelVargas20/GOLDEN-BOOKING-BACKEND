package com.sena.goldenbooking.reservasdeportivas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.reservasdeportivas.dto.EspacioDeportivoDto;
import com.sena.goldenbooking.reservasdeportivas.mapper.EspacioDeportivoMapperImpl;

class ImplementosSugeridosTest {

    @Test
    void sugiereSegunElDeporteSinImportarTildesNiMayusculas() {
        assertTrue(ImplementosSugeridos.para("FÚTBOL 5").contains("Balón"));
        assertTrue(ImplementosSugeridos.para("Pádel").contains("Palas"));
        assertTrue(ImplementosSugeridos.para("Natación").contains("Gorro"));
        assertTrue(ImplementosSugeridos.para("Deporte raro").contains("Hidratación"));
    }

    @Test
    void elEspacioUsaSuListaPropiaOLaSugerida() {
        EspacioDeportivoMapperImpl mapper = new EspacioDeportivoMapperImpl();
        EspacioDeportivo sinLista = EspacioDeportivo.builder().id("e1").deporte("Tenis").horaApertura(LocalTime.NOON).build();
        assertTrue(mapper.toDto(sinLista).getImplementos().contains("Raquetas"));

        EspacioDeportivo e = mapper.toEntity(EspacioDeportivoDto.builder().nombre("Cancha").deporte("Tenis")
                .implementos(List.of(" Raquetas ", "raquetas", "", "Pelotas")).build());
        assertEquals(List.of("Raquetas", "Pelotas"), e.getImplementos());
    }
}
