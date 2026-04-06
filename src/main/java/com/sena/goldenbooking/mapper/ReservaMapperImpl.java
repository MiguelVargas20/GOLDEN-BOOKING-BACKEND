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

    /**
     * Convierte de DTO a Modelo (Entidad para MongoDB)
     */
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

        // Lógica para objetos embebidos basada en el Tipo de Reserva
        if (dto.getTipo() != null) {
            if (dto.getTipo() == TipoReserva.HOTEL && dto.getDetalleHotel() != null) {
                ReservaHotel hotel = new ReservaHotel();
                hotel.setId(dto.getDetalleHotel().getId());
                hotel.setDatosH(dto.getDetalleHotel().getDatosH());
                reserva.setDetalleH(hotel);
                reserva.setDetalleD(null); // Aseguramos que el otro detalle sea nulo
            } 
            else if (dto.getTipo() == TipoReserva.DEPORTE && dto.getDetalleDeporte() != null) {
                ReservaDeporte deporte = new ReservaDeporte();
                deporte.setId(dto.getDetalleDeporte().getId());
                deporte.setTCancha(dto.getDetalleDeporte().getTCancha());
                deporte.setImpleAlqul(dto.getDetalleDeporte().getImpleAlqul());
                deporte.setRquireEntrenador(dto.getDetalleDeporte().isRquireEntrenador());
                reserva.setDetalleD(deporte);
                reserva.setDetalleH(null); // Aseguramos que el otro detalle sea nulo
            }
        }

        return reserva;
    }

    /**
     * Convierte de Modelo a DTO (Respuesta para el cliente)
     */
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

        // Mapeamos los detalles embebidos al DTO
        dto.setDetalleHotel(reserva.getDetalleH());
        dto.setDetalleDeporte(reserva.getDetalleD());

        return dto;
    }

    public List<ReservaDto> toDtoList(List<Reserva> reservas) {
        if (reservas == null) return null;
        return reservas.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una reserva existente (Útil para PUT)
     */
    public void actualizarReserva(ReservaDto dto, Reserva reserva) {
        if (dto == null || reserva == null) return;

        // Actualizamos datos comunes
        reserva.setEstR(dto.getEstado());
        reserva.setFechaI(dto.getFechaInicio());
        reserva.setFechaF(dto.getFechaFin());
        reserva.setPrecioT(dto.getPrecioTotal());

        // Actualización selectiva de los detalles anidados
        if (reserva.getTipR() == TipoReserva.HOTEL && dto.getDetalleHotel() != null) {
            if (reserva.getDetalleH() == null) reserva.setDetalleH(new ReservaHotel());
            reserva.getDetalleH().setDatosH(dto.getDetalleHotel().getDatosH());
        } 
        else if (reserva.getTipR() == TipoReserva.DEPORTE && dto.getDetalleDeporte() != null) {
            if (reserva.getDetalleD() == null) reserva.setDetalleD(new ReservaDeporte());
            reserva.getDetalleD().setTCancha(dto.getDetalleDeporte().getTCancha());
            reserva.getDetalleD().setImpleAlqul(dto.getDetalleDeporte().getImpleAlqul());
            reserva.getDetalleD().setRquireEntrenador(dto.getDetalleDeporte().isRquireEntrenador());
        }
    }
}