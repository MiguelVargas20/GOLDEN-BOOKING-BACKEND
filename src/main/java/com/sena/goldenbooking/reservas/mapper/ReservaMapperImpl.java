package com.sena.goldenbooking.reservas.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sena.goldenbooking.reservas.dto.ReservaDto;
import com.sena.goldenbooking.reservas.model.Reserva;

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
    //
    // FIX SEGURIDAD: antes este método copiaba estado, fechas y precio
    // directamente desde lo que mandaba el cliente en el body del PUT.
    // Eso permitía que CUALQUIER usuario dueño de una reserva (o incluso
    // un admin sin darse cuenta) cambiara el precio a $1 o pusiera fechas
    // que ya estén ocupadas, porque crear() sí valida solapamiento con el
    // lock, pero actualizar() nunca repetía esa validación.
    //
    // El estado ya tiene su propio flujo correcto y controlado por el
    // servidor (PATCH /cancelar en ReservaController), así que no debe
    // tocarse aquí. Fechas y precio no tienen hoy un endpoint de
    // "reprogramar" que re-valide solapamiento y recalcule el precio en
    // el servidor — hasta que exista, no se aceptan cambios de esos
    // campos vía este PUT.
    @Override
    public void actualizarReserva(ReservaDto dto, Reserva reserva) {

        // Validamos que ni el DTO ni la Reserva sean nulos antes de actualizar
        if (dto == null || reserva == null) return;
        // Sin campos seguros que actualizar por ahora: no se modifica nada.
        // (Se deja el método y la validación de permisos/IDOR intactas en el
        // service, por si en el futuro se agrega un campo legítimo editable.)
    }
}