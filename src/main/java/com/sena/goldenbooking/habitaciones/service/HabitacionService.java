package com.sena.goldenbooking.habitaciones.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.habitaciones.dto.HabitacionDto;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;

public interface HabitacionService {
    
    /* Método para crear una nueva habitación a partir de un DTO */
    HabitacionDto crear(HabitacionDto dto);
    
    /* Método para listar habitaciones por estado, devolviendo una lista de DTOs */
    List<HabitacionDto> listarPorEstado(EstadoHabitacion estado);
    
    // Usamos el método que corregimos en el repositorio
    List<HabitacionDto> listarPorTipo(String idTipoHabitacion);
    
    /* Método para obtener una habitación por su ID, devolviendo un DTO */
    HabitacionDto obtenerPorId(String id);
    
    /* Método para actualizar una habitación existente a partir de un DTO, devolviendo el DTO actualizado */
    HabitacionDto actualizar(String id, HabitacionDto dto);
    
    /* Método para cambiar el estado de una habitación, devolviendo el DTO actualizado */
    HabitacionDto cambiarEstado(String id, EstadoHabitacion nuevoEstado);
    
    /* Método para eliminar una habitación por su ID */
    void eliminar(String id);

    Page<HabitacionDto> listarTodasPaginadas(Pageable pageable);

    /** Sube o reemplaza la imagen de la habitación (ADMIN). */
    HabitacionDto subirImagen(String id, MultipartFile archivo);

    /** Quita la imagen subida: vuelve a la imagen por defecto. */
    HabitacionDto eliminarImagen(String id);

    /** Archivo de la imagen para enviarlo al navegador. */
    GridFsResource obtenerImagen(String id);

    /** Agrega una imagen a la galería (máximo 5). */
    HabitacionDto agregarImagen(String id, MultipartFile archivo);

    /** Quita una imagen de la galería. */
    HabitacionDto quitarImagen(String id, String imagenId);

    /** Pone esa imagen de primera (portada del catálogo). */
    HabitacionDto elegirPortada(String id, String imagenId);

    /** Archivo de una imagen de la galería. */
    GridFsResource obtenerImagenGaleria(String id, String imagenId);
}