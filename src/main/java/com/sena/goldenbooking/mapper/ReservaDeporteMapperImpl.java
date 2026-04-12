package com.sena.goldenbooking.mapper;

import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.models.ReservaDeporte;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

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
                .tCancha(rd.getTipoCancha())
                .implAlquilados(rd.getImplementosAlquilados())
                .rqrEntrenador(rd.isRequiereEntrenador())
                .fInicioReserva(rd.getFechaReserva())
                .fFinReserva(rd.getFechaFinReserva())
                .pr(rd.getPrecio())
                .build();
    }
    // Mapeo de lista de ReservaDeporte a lista de ReservaDeporteDto
    @Override
    public List<ReservaDeporteDto> toDtoList(List<ReservaDeporte> lista) {
        if (lista == null) return null;
        return lista.stream().map(this::toDto).collect(Collectors.toList());
    }


    // Actualizar una ReservaDeporte existente con datos de un ReservaDeporteDto
    @Override
    public void actualizarReservaDeporte(ReservaDeporteDto dto, ReservaDeporte rd) {
        if (dto == null || rd == null) return;
        rd.setTipoCancha(dto.getTCancha());
        rd.setImplementosAlquilados(dto.getImplAlquilados());
        rd.setRequiereEntrenador(dto.isRqrEntrenador());
        rd.setFechaReserva(dto.getFInicioReserva());
        rd.setFechaFinReserva(dto.getFFinReserva());
        rd.setPrecio(dto.getPr());
    }
}