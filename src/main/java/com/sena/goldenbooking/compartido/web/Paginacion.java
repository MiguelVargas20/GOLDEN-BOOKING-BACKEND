package com.sena.goldenbooking.compartido.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Construye el Pageable de los listados paginados con límites seguros.
 *
 * Antes cada controller hacía PageRequest.of(page, size) directo con lo que
 * mandara el cliente: ?size=1000000 traía la colección completa de Mongo en
 * una sola respuesta (lento y costoso en memoria), y ?page=-1 o ?size=0
 * reventaban con IllegalArgumentException.
 */
public final class Paginacion {

    /** Máximo de elementos por página que se aceptan. */
    public static final int TAMANIO_MAXIMO = 100;

    private Paginacion() {
        // Clase de utilidades: no se instancia
    }

    public static Pageable de(int page, int size) {
        int paginaSegura = Math.max(page, 0);
        int tamanioSeguro = Math.min(Math.max(size, 1), TAMANIO_MAXIMO);
        return PageRequest.of(paginaSegura, tamanioSeguro);
    }
}
