package com.sena.goldenbooking.reservas.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reservas.model.MiembroReserva;

/** Reglas de los acompañantes de una reserva (hotel y deporte). */
public final class ReglasMiembros {

    private ReglasMiembros() {
        // Clase de utilidades: no se instancia
    }

    /**
     * Limpia y valida la lista: sin documentos repetidos, sin repetir al
     * titular y sin superar la capacidad (el titular ocupa un cupo).
     * @return la lista limpia (vacía si no se envió ninguno)
     */
    public static List<MiembroReserva> validar(List<MiembroReserva> miembros, String docTitular, Integer capacidad) {
        if (miembros == null || miembros.isEmpty()) return new ArrayList<>();
        int maximo = capacidad != null ? Math.max(0, capacidad - 1) : Integer.MAX_VALUE;
        if (miembros.size() > maximo) {
            throw new SolicitudInvalidaException("Puedes registrar máximo " + maximo
                    + (maximo == 1 ? " acompañante" : " acompañantes") + " (capacidad de " + capacidad + " personas contándote a ti).");
        }
        Set<String> documentos = new HashSet<>();
        List<MiembroReserva> limpios = new ArrayList<>();
        for (MiembroReserva m : miembros) {
            if (m == null || m.getNumeroDocumento() == null || m.getNombre() == null || m.getTipoDocumento() == null) {
                throw new SolicitudInvalidaException("Completa el nombre, tipo y número de documento de cada acompañante.");
            }
            String numero = m.getNumeroDocumento().trim();
            if (numero.equals(docTitular)) {
                throw new SolicitudInvalidaException("El titular de la reserva no se registra como acompañante.");
            }
            if (!documentos.add(numero)) {
                throw new SolicitudInvalidaException("El documento " + numero + " está repetido entre los acompañantes.");
            }
            limpios.add(MiembroReserva.builder()
                    .nombre(m.getNombre().trim().replaceAll("\\s+", " "))
                    .tipoDocumento(m.getTipoDocumento().trim())
                    .numeroDocumento(numero)
                    .build());
        }
        return limpios;
    }
}
