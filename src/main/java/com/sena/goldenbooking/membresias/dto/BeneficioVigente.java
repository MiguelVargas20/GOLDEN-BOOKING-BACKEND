package com.sena.goldenbooking.membresias.dto;

import java.time.LocalDate;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;

/** Beneficios que aplican hoy a un cliente según su categoría. */
public record BeneficioVigente(TipoMembresia membresia, double descuento, int diasAnticipacion) {

    /** Sin membresía, sin descuento y sin límite: para cuando no se puede consultar (p. ej. pruebas). */
    public static BeneficioVigente sinBeneficios(int diasAnticipacion) {
        return new BeneficioVigente(TipoMembresia.NINGUNA, 0, diasAnticipacion);
    }

    /**
     * El cliente solo puede reservar hasta "diasAnticipacion" días desde hoy
     * (los socios tienen más días: es uno de sus beneficios).
     */
    public void validarAnticipacion(LocalDate fecha) {
        LocalDate limite = ZonaHoraria.ahora().toLocalDate().plusDays(diasAnticipacion);
        if (fecha.isAfter(limite)) {
            throw new SolicitudInvalidaException("Puedes reservar con máximo " + diasAnticipacion + " días de anticipación"
                    + (membresia == TipoMembresia.NINGUNA ? ". Los socios pueden reservar con más anticipación." : "."));
        }
    }

    /** Precio con el descuento aplicado, redondeado a pesos. */
    public double aplicar(double precio) {
        return aplicar(precio, descuento);
    }

    /** Precio con un descuento (porcentaje) ya guardado en la reserva. */
    public static double aplicar(double precio, Double descuento) {
        return descuento == null || descuento <= 0 ? Math.round(precio) : Math.round(precio * (1 - descuento / 100.0));
    }

    /** Descuento para guardar en la reserva (null si no hay). */
    public Double descuentoParaGuardar() {
        return descuento > 0 ? descuento : null;
    }
}
