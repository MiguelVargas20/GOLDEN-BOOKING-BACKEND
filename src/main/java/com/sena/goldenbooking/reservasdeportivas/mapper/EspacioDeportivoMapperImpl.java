package com.sena.goldenbooking.reservasdeportivas.mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sena.goldenbooking.reservasdeportivas.dto.EspacioDeportivoDto;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.ImplementosSugeridos;

@Component
public class EspacioDeportivoMapperImpl implements EspacioDeportivoMapper {

    /**
     * Ruta pública desde la que el frontend carga la imagen subida (<img src>).
     * "?v=" lleva el id del archivo: cuando el admin sube otra imagen la URL
     * cambia, así el navegador no muestra la vieja desde su caché.
     */
    private static final String RUTA_IMAGEN = "/api/espacios-deportivos/%s/imagen?v=%s";

    @Override
    public EspacioDeportivo toEntity(EspacioDeportivoDto dto) {
        if (dto == null) return null;
        return EspacioDeportivo.builder()
                .nombre(limpiar(dto.getNombre()))
                .deporte(limpiar(dto.getDeporte()))
                .descripcion(dto.getDescripcion())
                .capacidad(dto.getCapacidad())
                .tarifaHora(dto.getTarifaHora())
                .horaApertura(dto.getHoraApertura())
                .horaCierre(dto.getHoraCierre())
                .estado(dto.getEstado())
                .implementos(limpiarLista(dto.getImplementos()))
                .build();
    }

    @Override
    public EspacioDeportivoDto toDto(EspacioDeportivo e) {
        if (e == null) return null;
        return EspacioDeportivoDto.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .deporte(e.getDeporte())
                .descripcion(e.getDescripcion())
                .capacidad(e.getCapacidad())
                .tarifaHora(e.getTarifaHora())
                .horaApertura(e.getHoraApertura())
                .horaCierre(e.getHoraCierre())
                .estado(e.getEstado())
                .implementos(e.getImplementos() != null && !e.getImplementos().isEmpty()
                        ? e.getImplementos() : ImplementosSugeridos.para(e.getDeporte()))
                .imagenUrl(e.getImagenId() != null ? RUTA_IMAGEN.formatted(e.getId(), e.getImagenId()) : null)
                .fechaCreacion(e.getFechaCreacion())
                .fechaActualizacion(e.getFechaActualizacion())
                .build();
    }

    @Override
    public List<EspacioDeportivoDto> toDtoList(List<EspacioDeportivo> espacios) {
        if (espacios == null) return List.of();
        return espacios.stream().map(this::toDto).toList();
    }

    @Override
    public void actualizar(EspacioDeportivoDto dto, EspacioDeportivo espacio) {
        if (dto == null || espacio == null) return;
        espacio.setNombre(limpiar(dto.getNombre()));
        espacio.setDeporte(limpiar(dto.getDeporte()));
        espacio.setDescripcion(dto.getDescripcion());
        espacio.setCapacidad(dto.getCapacidad());
        espacio.setTarifaHora(dto.getTarifaHora());
        espacio.setHoraApertura(dto.getHoraApertura());
        espacio.setHoraCierre(dto.getHoraCierre());
        if (dto.getEstado() != null) {
            espacio.setEstado(dto.getEstado());
        }
        if (dto.getImplementos() != null) {
            espacio.setImplementos(limpiarLista(dto.getImplementos()));
        }
    }

    /** Sin vacíos ni repetidos (sin importar mayúsculas). */
    private static List<String> limpiarLista(List<String> lista) {
        if (lista == null) return null;
        Set<String> vistos = new HashSet<>();
        return lista.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty() && vistos.add(t.toLowerCase(Locale.ROOT)))
                .toList();
    }

    /** Quita espacios sobrantes para que "Cancha  1 " y "Cancha 1" no sean nombres distintos. */
    private String limpiar(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ");
    }
}
