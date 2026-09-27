package com.sena.goldenbooking.cargos.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.sena.goldenbooking.cargos.model.CategoriaCargo;
import com.sena.goldenbooking.cargos.model.DestinoCargo;
import com.sena.goldenbooking.cargos.model.EstadoCargo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Consumo cargado a una reserva o a la cuenta de socio. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Consumo del cliente cargado a una reserva activa o a su cuenta de socio.")
public class CargoDto {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "Indica el documento del cliente.")
    @Schema(example = "52123456")
    private String docUsuario;

    @NotBlank(message = "Indica qué consumió el cliente.")
    @Size(min = 2, max = 80, message = "El concepto debe tener entre 2 y 80 caracteres.")
    @Schema(example = "Agua sin gas 600 ml")
    private String concepto;

    @NotNull(message = "Indica la categoría del consumo.")
    private CategoriaCargo categoria;

    @Min(value = 1, message = "La cantidad mínima es 1.")
    @Max(value = 100, message = "La cantidad máxima es 100.")
    @Schema(example = "2")
    private int cantidad;

    @Positive(message = "El valor unitario debe ser mayor a cero.")
    @Max(value = 10_000_000, message = "El valor unitario es demasiado alto.")
    @Schema(example = "4500")
    private double valorUnitario;

    @NotNull(message = "Indica a qué se carga: una reserva o la cuenta de socio.")
    private DestinoCargo destino;

    @Schema(description = "Id de la reserva (obligatorio si el destino es una reserva).")
    private String idReserva;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private double total;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String descripcionDestino;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String nombreCliente;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private EstadoCargo estado;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fecha;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String registradoPor;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaPago;
}
