package com.sena.goldenbooking.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reservas.model.MiembroReserva;

class ReglasMiembrosTest {

    private static MiembroReserva m(String nombre, String tipo, String doc) {
        return MiembroReserva.builder().nombre(nombre).tipoDocumento(tipo).numeroDocumento(doc).build();
    }

    @Test
    void limpiaEspaciosYAceptaMenoresConTarjetaDeIdentidad() {
        List<MiembroReserva> r = ReglasMiembros.validar(List.of(m("  Sofía   Pérez ", "TI", " 1023456789 ")), "52123456", 4);
        assertEquals("Sofía Pérez", r.get(0).getNombre());
        assertEquals("1023456789", r.get(0).getNumeroDocumento());
    }

    @Test
    void sinAcompanantesDevuelveListaVacia() {
        assertTrue(ReglasMiembros.validar(null, "1", 4).isEmpty());
    }

    @Test
    void elTitularOcupaUnCupo() {
        List<MiembroReserva> tres = List.of(m("A B", "CC", "11111"), m("C D", "CC", "22222"), m("E F", "TI", "33333"));
        assertEquals(3, ReglasMiembros.validar(tres, "1", 4).size());
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class, () -> ReglasMiembros.validar(tres, "1", 3));
        assertTrue(ex.getMessage().contains("máximo 2 acompañantes"));
    }

    @Test
    void noSeRepitenDocumentosNiElDelTitular() {
        assertThrows(SolicitudInvalidaException.class,
                () -> ReglasMiembros.validar(List.of(m("A B", "CC", "11111"), m("C D", "TI", "11111")), "1", 10));
        assertThrows(SolicitudInvalidaException.class,
                () -> ReglasMiembros.validar(List.of(m("A B", "CC", "52123456")), "52123456", 10));
    }
}
