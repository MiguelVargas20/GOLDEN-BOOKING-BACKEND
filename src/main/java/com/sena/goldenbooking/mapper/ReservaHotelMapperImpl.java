package com.sena.goldenbooking.mapper;

import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.models.ReservaHotel;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReservaHotelMapperImpl implements ReservaHotelMapper {

    // Mapeo entre ReservaHotel y ReservaHotelDto
    @Override
    public ReservaHotel toReservaHotel(ReservaHotelDto dto) {
        if (dto == null) return null;

        // Usamos el patrón builder para crear una instancia de ReservaHotel a partir de ReservaHotelDto
        return ReservaHotel.builder()
                .idHotelReserva(dto.getIdH())
                .idReserva(dto.getIdH())        // se asigna al guardar en el service
                .idHabitacion(dto.getIdHabitacion())// datosH se llena en el service buscando la habitación por id
                .fechaCheckIn(dto.getFCheckIn())
                .fechaCheckOut(dto.getFCheckOut())
                .noches(dto.getNoch())
                .precioTotal(dto.getPTotal())
                .build();
    }

    // Mapeo de ReservaHotel a ReservaHotelDto
    
    @Override
    public ReservaHotelDto toDto(ReservaHotel rh) {
        if (rh == null) return null;

        ReservaHotelDto dto = ReservaHotelDto.builder()
                .idH(rh.getIdHotelReserva())
                .docUsuario(rh.getDocUsuario())    // ← agregar esta línea
                .idHabitacion(rh.getIdHabitacion())
                .fCheckIn(rh.getFechaCheckIn())
                .fCheckOut(rh.getFechaCheckOut())
                .noch(rh.getNoches())
                .pTotal(rh.getPrecioTotal())
                .build();

        if (rh.getDatosH() != null) {
            dto.setNumeroHabitacion(rh.getDatosH().getNumHab());
            dto.setTHabitacion(rh.getDatosH().getTipoHabitacion() != null
                    ? rh.getDatosH().getTipoHabitacion().getNomTipo() : null);
            dto.setPNoche(rh.getDatosH().getPrecNoche());
            dto.setEstHabitacion(rh.getDatosH().getEstado());
        }

        return dto;
    }
    
    // Mapeo de lista de ReservaHotel a lista de ReservaHotelDto
    @Override
    public List<ReservaHotelDto> toDtoList(List<ReservaHotel> lista) {
        if (lista == null) return null;
        return lista.stream().map(this::toDto).collect(Collectors.toList());
    }


    // Actualizar una ReservaHotel existente con datos de un ReservaHotelDto
    @Override
    public void actualizarReservaHotel(ReservaHotelDto dto, ReservaHotel rh) {

        // Validamos que ni el DTO ni la ReservaHotel sean nulos antes de actualizar
        if (dto == null || rh == null) return;
        rh.setFechaCheckIn(dto.getFCheckIn());
        rh.setFechaCheckOut(dto.getFCheckOut());
        rh.setNoches(dto.getNoch());
        rh.setPrecioTotal(dto.getPTotal());
    }
}