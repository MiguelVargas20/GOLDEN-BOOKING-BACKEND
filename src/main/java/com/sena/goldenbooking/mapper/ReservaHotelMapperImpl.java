package com.sena.goldenbooking.mapper;

import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.models.EstadoReserva;
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
                .estado(rh.getEstado())
                .fechaSolicitud(rh.getFechaSolicitud())
                .fechaConfirmacion(rh.getFechaConfirmacion())
                .fechaCancelacion(rh.getFechaCancelacion())
                .canceladaPor(rh.getCanceladaPor())
                .motivoCancelacion(rh.getMotivoCancelacion())
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
    //
    // FIX SEGURIDAD: este método copiaba fechaCheckIn/fechaCheckOut/noches/
    // precioTotal directamente desde el DTO que manda el cliente. crear()
    // sí valida solapamiento de fechas dentro del lock por habitación, pero
    // actualizar() nunca repetía esa validación ni recalculaba el precio.
    // Resultado: un cliente autenticado podía hacer PUT sobre SU PROPIA
    // reserva con {"pTotal": 1, "fCheckIn": "...", "fCheckOut": "..."} y
    // quedarse con una reserva a precio arbitrario, en fechas que incluso
    // podían ya estar ocupadas por otra reserva.
    //
    // Hasta que exista un endpoint de "reprogramar" dedicado que repita la
    // validación de solapamiento y recalcule el precio en el servidor (nunca
    // confiando en lo que mande el cliente), este PUT no debe tocar esos
    // campos.
    @Override
    public void actualizarReservaHotel(ReservaHotelDto dto, ReservaHotel rh) {

        // Validamos que ni el DTO ni la ReservaHotel sean nulos antes de actualizar
        if (dto == null || rh == null) return;
        // Sin campos seguros que actualizar por ahora: no se modifica nada.
    }
}