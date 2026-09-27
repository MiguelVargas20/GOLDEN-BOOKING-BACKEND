package com.sena.goldenbooking.reservasdeportivas.service;

import java.util.List;

import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.reservasdeportivas.dto.EspacioDeportivoDto;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;

public interface EspacioDeportivoService {

    /**
     * Lista los espacios. El admin ve todos; el cliente ve los ACTIVOS y los
     * que están en MANTENIMIENTO (para mostrarlos como "no disponibles"),
     * nunca los INACTIVOS.
     */
    List<EspacioDeportivoDto> listar(boolean incluirInactivos);

    EspacioDeportivoDto obtenerPorId(String id);

    /** Entidad lista para reservar: debe existir y estar ACTIVA (lo usa ReservaDeporteService). */
    EspacioDeportivo obtenerReservable(String id);

    EspacioDeportivoDto crear(EspacioDeportivoDto dto);

    EspacioDeportivoDto actualizar(String id, EspacioDeportivoDto dto);

    EspacioDeportivoDto cambiarEstado(String id, EstadoEspacio estado);

    /** Elimina el espacio. Si tiene historial de reservas se pide marcarlo INACTIVO en su lugar. */
    void eliminar(String id);

    /** Sube (o reemplaza) la imagen del espacio. Acepta JPG, PNG o WEBP de hasta 5 MB. */
    EspacioDeportivoDto subirImagen(String id, MultipartFile archivo);

    /** Quita la imagen subida: el espacio vuelve a usar la imagen por defecto de su deporte. */
    EspacioDeportivoDto eliminarImagen(String id);

    /** Imagen subida del espacio, para servirla al navegador. */
    GridFsResource obtenerImagen(String id);
}
