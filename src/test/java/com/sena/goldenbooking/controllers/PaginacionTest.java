package com.sena.goldenbooking.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PaginacionTest {

    @Test
    void respetaValoresNormales() {
        Pageable p = Paginacion.de(2, 10);
        assertEquals(2, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void limitaElTamanioMaximo() {
        assertEquals(Paginacion.TAMANIO_MAXIMO, Paginacion.de(0, 1_000_000).getPageSize());
    }

    @Test
    void corrigeValoresInvalidosEnVezDeFallar() {
        Pageable p = Paginacion.de(-1, 0);
        assertEquals(0, p.getPageNumber());
        assertEquals(1, p.getPageSize());
    }
}
