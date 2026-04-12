package com.sena.goldenbooking.mapper;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.Reserva;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component

// Implementación del mapper para Reserva
public class ReservaMapperImpl implements ReservaMapper {

    // Mapeo entre Reserva y ReservaDto
    @Override
    public Reserva toReserva(ReservaDto dto) {
        if (dto == null) return null;

        // Usamos el patrón builder para crear una instancia de Reserva a partir de ReservaDto
        return Reserva.builder()
                .id(dto.getIdR())
                .documentoUsuario(dto.getDocUsuario())
                .tipo(dto.getTp())
                .estado(dto.getEst())
                .fechaReserva(dto.getFReserva())
                .fechaInicio(dto.getFInicio())
                .fechaFin(dto.getFFin())
                .precioTotal(dto.getPTotal())
                .build();
    }

    // Mapeo de Reserva a ReservaDto
    @Override

    // Mapeo de Reserva a ReservaDto
    public ReservaDto toDto(Reserva reserva) {
        if (reserva == null) return null;


        // Usamos el patrón builder para crear una instancia de ReservaDto a partir de Reserva
        return ReservaDto.builder()
                .idR(reserva.getId())
                .docUsuario(reserva.getDocumentoUsuario())
                .tp(reserva.getTipo())
                .est(reserva.getEstado())
                .fReserva(reserva.getFechaReserva())
                .fInicio(reserva.getFechaInicio())
                .fFin(reserva.getFechaFin())
                .pTotal(reserva.getPrecioTotal())
                .build();
    }


    // Mapeo de lista de Reservas a lista de ReservaDtos
    @Override
    public List<ReservaDto> toDtoList(List<Reserva> reservas) {
        if (reservas == null) return null;
        return reservas.stream().map(this::toDto).collect(Collectors.toList());
    }

    // Actualizar una Reserva existente con datos de un ReservaDto
    @Override
    public void actualizarReserva(ReservaDto dto, Reserva reserva) {

        // Validamos que ni el DTO ni la Reserva sean nulos antes de actualizar
        if (dto == null || reserva == null) return;
        reserva.setEstado(dto.getEst());
        reserva.setFechaInicio(dto.getFInicio());
        reserva.setFechaFin(dto.getFFin());
        reserva.setPrecioTotal(dto.getPTotal());
    }
}