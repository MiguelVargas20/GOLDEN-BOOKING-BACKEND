package com.sena.goldenbooking.membresias.dto;

import com.sena.goldenbooking.membresias.model.BeneficiosMembresia;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;

/** Lo que ve el cliente en su perfil: su categoría, beneficios y avance. */
public record MiMembresiaDto(
        TipoMembresia membresia,
        double descuento,
        int diasAnticipacion,
        String otrosBeneficios,
        long reservas,
        int reservasParaSugerir,
        BeneficiosMembresia ocasional,
        BeneficiosMembresia miembro) {
}
