package com.sena.goldenbooking.reservashoteleras.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** Choque entre estadías: se compara por día, no por hora. */
class SolapamientoHotelTest {

    private static LocalDateTime dia(int d, int hora) {
        return LocalDate.of(2026, 9, d).atTime(hora, 0);
    }

    @Test
    void salirYEntrarElMismoDiaNoEsChoqueAunqueLasHorasSeanViejas() {
        // Reserva antigua guardada con la medianoche corrida a UTC (05:00)
        // y una nueva que entra el día en que la antigua sale
        assertFalse(ReservaHotelServiceImpl.seSolapan(dia(27, 5), dia(30, 5), dia(30, 15), LocalDate.of(2026, 10, 2).atTime(12, 0)));
    }

    @Test
    void estadiasQueSeCruzanSiChocan() {
        assertTrue(ReservaHotelServiceImpl.seSolapan(dia(27, 15), dia(30, 12), dia(29, 15), LocalDate.of(2026, 10, 2).atTime(12, 0)));
    }

    @Test
    void estadiasSeparadasNoChocan() {
        assertFalse(ReservaHotelServiceImpl.seSolapan(dia(1, 15), dia(3, 12), dia(5, 15), dia(7, 12)));
    }
}
