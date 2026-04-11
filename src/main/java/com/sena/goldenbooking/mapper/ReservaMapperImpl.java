package com.sena.goldenbooking.mapper;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaHotel;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.models.TipoReserva;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReservaMapperImpl implements ReservaMapper {

    @Override
    public Reserva toReserva(ReservaDto dto) {
        if (dto == null) return null;

        Reserva reserva = new Reserva();
        reserva.setId(dto.getId());
        reserva.setDocUsuario(dto.getDocumentoUsuario());
        reserva.setFechaR(dto.getFechaReserva());
        reserva.setEstR(dto.getEstado());
        reserva.setTipR(dto.getTipo());
        reserva.setFechaI(dto.getFechaInicio());
        reserva.setFechaF(dto.getFechaFin());
        reserva.setPrecioT(dto.getPrecioTotal());

        if (dto.getTipo() == TipoReserva.HOTEL && dto.getDetalleHotel() != null) {
            ReservaHotel hotel = new ReservaHotel();
            hotel.setDatosH(dto.getDetalleHotel().getDatosH());
            reserva.setDetalleH(hotel);
            reserva.setDetalleD(null);

        } else if (dto.getTipo() == TipoReserva.DEPORTE && dto.getDetalleDeporte() != null) {
            ReservaDeporte deporte = new ReservaDeporte();
            deporte.setTCancha(dto.getDetalleDeporte().getTCancha());
            deporte.setImpleAlqul(dto.getDetalleDeporte().getImpleAlqul());
            deporte.setRquireEntrenador(dto.getDetalleDeporte().isRquireEntrenador());
            reserva.setDetalleD(deporte);
            reserva.setDetalleH(null);
        }

        return reserva;
    }

    @Override
    public ReservaDto toDto(Reserva reserva) {
        if (reserva == null) return null;

        ReservaDto dto = new ReservaDto();
        dto.setId(reserva.getId());
        dto.setDocumentoUsuario(reserva.getDocUsuario());
        dto.setFechaReserva(reserva.getFechaR());
        dto.setEstado(reserva.getEstR());
        dto.setTipo(reserva.getTipR());
        dto.setFechaInicio(reserva.getFechaI());
        dto.setFechaFin(reserva.getFechaF());
        dto.setPrecioTotal(reserva.getPrecioT());
        dto.setDetalleHotel(reserva.getDetalleH());
        dto.setDetalleDeporte(reserva.getDetalleD());

        return dto;
    }

    @Override
    public List<ReservaDto> toDtoList(List<Reserva> reservas) {
        if (reservas == null) return null;
        return reservas.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void actualizarReserva(ReservaDto dto, Reserva reserva) {
        if (dto == null || reserva == null) return;

        reserva.setEstR(dto.getEstado());
        reserva.setFechaI(dto.getFechaInicio());
        reserva.setFechaF(dto.getFechaFin());
        reserva.setPrecioT(dto.getPrecioTotal());

        if (reserva.getTipR() == TipoReserva.HOTEL && dto.getDetalleHotel() != null) {
            if (reserva.getDetalleH() == null) reserva.setDetalleH(new ReservaHotel());
            reserva.getDetalleH().setDatosH(dto.getDetalleHotel().getDatosH());

        } else if (reserva.getTipR() == TipoReserva.DEPORTE && dto.getDetalleDeporte() != null) {
            if (reserva.getDetalleD() == null) reserva.setDetalleD(new ReservaDeporte());
            reserva.getDetalleD().setTCancha(dto.getDetalleDeporte().getTCancha());
            reserva.getDetalleD().setImpleAlqul(dto.getDetalleDeporte().getImpleAlqul());
            reserva.getDetalleD().setRquireEntrenador(dto.getDetalleDeporte().isRquireEntrenador());
        }
    }
}