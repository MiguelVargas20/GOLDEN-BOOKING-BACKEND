package com.sena.goldenbooking.reservasdeportivas.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sena.goldenbooking.reservasdeportivas.dto.ReservaDeporteDto;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;

@Component
public class ReservaDeporteMapperImpl implements ReservaDeporteMapper {

// Mapeo entre ReservaDeporte y ReservaDeporteDto
    @Override
    public ReservaDeporte toReservaDeporte(ReservaDeporteDto dto) {
        if (dto == null) return null;

        // Usamos el patrón builder para crear una instancia de ReservaDeporte a partir de ReservaDeporteDto
        return ReservaDeporte.builder()
                .idReservaDeporte(dto.getIdD())
                .idReserva(dto.getIdD())        // se asigna al guardar en el service
                .espacioId(dto.getEspacioId())
                .tipoCancha(dto.getTCancha())
                .implementosAlquilados(dto.getImplAlquilados())
                .requiereEntrenador(dto.isRqrEntrenador())
                .fechaReserva(dto.getFInicioReserva())
                .fechaFinReserva(dto.getFFinReserva())
                .precio(dto.getPr())
                .build();
    }


    // Mapeo de ReservaDeporte a ReservaDeporteDto
    @Override
    public ReservaDeporteDto toDto(ReservaDeporte rd) {
        if (rd == null) return null;

        return ReservaDeporteDto.builder()
                .idD(rd.getIdReservaDeporte())
                .docUsuario(rd.getDocUsuario())      // ← esta línea faltaba
                .espacioId(rd.getEspacioId())
                .tCancha(rd.getTipoCancha())
                .implAlquilados(rd.getImplementosAlquilados())
                .rqrEntrenador(rd.isRequiereEntrenador())
                .fInicioReserva(rd.getFechaReserva())
                .fFinReserva(rd.getFechaFinReserva())
                .pr(rd.getPrecio())
                .estado(rd.getEstado())
                .registradaPorAdministrador(rd.isRegistradaPorAdministrador())
                .fechaSolicitud(rd.getFechaSolicitud())
                .fechaConfirmacion(rd.getFechaConfirmacion())
                .fechaCancelacion(rd.getFechaCancelacion())
                .canceladaPor(rd.getCanceladaPor())
                .motivoCancelacion(rd.getMotivoCancelacion())
                .build();
    }
    // Mapeo de lista de ReservaDeporte a lista de ReservaDeporteDto
    @Override
    public List<ReservaDeporteDto> toDtoList(List<ReservaDeporte> lista) {
        if (lista == null) return null;
        return lista.stream().map(this::toDto).collect(Collectors.toList());
    }


    // Actualizar una ReservaDeporte existente con datos de un ReservaDeporteDto
    //
    // FIX SEGURIDAD: igual que en ReservaHotelMapperImpl — fechaReserva,
    // fechaFinReserva y precio venían directo del DTO del cliente sin volver
    // a validar solapamiento de cancha/horario ni recalcular el precio en
    // el servidor. Esos campos quedan bloqueados en este PUT hasta que
    // exista un endpoint de reprogramación que repita esa validación.
    //
    // CORRECCIÓN sobre la primera versión de este fix: tipoCancha también
    // se sacó de aquí. Cambiar de cancha en el mismo horario es exactamente
    // el mismo tipo de cambio que cambiar la fecha — mueve la reserva a un
    // recurso distinto sin volver a pasar por el lock de solapamiento de
    // crear(), así que dejarlo editable habría sido la misma vulnerabilidad
    // con otro nombre.
    //
    // implementosAlquilados y requiereEntrenador sí son datos seguros de
    // dejar editar aquí: son extras que no afectan disponibilidad ni el
    // precio ya calculado.
    @Override
    public void actualizarReservaDeporte(ReservaDeporteDto dto, ReservaDeporte rd) {
        if (dto == null || rd == null) return;
        rd.setImplementosAlquilados(dto.getImplAlquilados());
        rd.setRequiereEntrenador(dto.isRqrEntrenador());
    }
}