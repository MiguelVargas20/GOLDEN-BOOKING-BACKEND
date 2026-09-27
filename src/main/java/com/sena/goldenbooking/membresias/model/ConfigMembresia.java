package com.sena.goldenbooking.membresias.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reglas del programa de socios (un solo documento, lo edita el admin en el panel de socios). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "ConfigMembresia")
@Schema(description = "Configuración del programa de socios.")
public class ConfigMembresia {

    public static final String ID = "config";

    @Id
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Min(value = 1, message = "Debe ser al menos 1 reserva.")
    @Max(value = 500, message = "Máximo 500 reservas.")
    @Schema(description = "Reservas (confirmadas o finalizadas) a partir de las cuales se sugiere hacer socio al cliente.", example = "5")
    private int reservasParaSugerir;

    @Min(value = 1, message = "La anticipación mínima es 1 día.")
    @Max(value = 730, message = "La anticipación máxima es 730 días.")
    @Schema(description = "Días de anticipación con los que reserva un cliente sin membresía.", example = "60")
    private int diasAnticipacionGeneral;

    @NotNull(message = "Faltan los beneficios de la categoría Ocasional.")
    @Valid
    private BeneficiosMembresia ocasional;

    @NotNull(message = "Faltan los beneficios de la categoría Miembro.")
    @Valid
    private BeneficiosMembresia miembro;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaActualizacion;

    /** Valores iniciales mientras el admin no configure nada. */
    public static ConfigMembresia porDefecto() {
        return ConfigMembresia.builder()
                .id(ID)
                .reservasParaSugerir(5)
                .diasAnticipacionGeneral(60)
                .ocasional(BeneficiosMembresia.builder().descuento(5).diasAnticipacion(120)
                        .otrosBeneficios("Reserva con más anticipación.").build())
                .miembro(BeneficiosMembresia.builder().descuento(10).diasAnticipacion(365)
                        .otrosBeneficios("Cuenta de socio para cargar consumos y pagar a fin de mes.").build())
                .build();
    }
}
