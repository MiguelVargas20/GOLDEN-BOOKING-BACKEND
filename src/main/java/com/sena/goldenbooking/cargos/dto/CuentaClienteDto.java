package com.sena.goldenbooking.cargos.dto;

import java.util.List;

import com.sena.goldenbooking.usuarios.model.TipoMembresia;

/** Estado de cuenta del cliente: consumos, total pendiente y a qué se pueden cargar nuevos. */
public record CuentaClienteDto(
        String docUsuario,
        String nombreCliente,
        TipoMembresia membresia,
        double totalPendiente,
        List<DestinoDisponibleDto> destinos,
        List<CargoDto> cargos) {
}
