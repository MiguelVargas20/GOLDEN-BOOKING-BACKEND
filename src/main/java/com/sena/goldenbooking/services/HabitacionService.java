package com.sena.goldenbooking.services;

import java.util.List;
import com.sena.goldenbooking.dtos.HabitacionDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HabitacionService {
    
    /* Método para crear una nueva habitación a partir de un DTO */
    HabitacionDto crear(HabitacionDto dto);
    
    /* Método para listar todas las habitaciones, devolviendo una lista de DTOs */
    List<HabitacionDto> listarTodas();
    
    /* Método para listar habitaciones por estado, devolviendo una lista de DTOs */
    List<HabitacionDto> listarPorEstado(String estado);
    
    // Usamos el método que corregimos en el repositorio
    List<HabitacionDto> listarPorTipo(String idTipoHabitacion);
    
    /* Método para obtener una habitación por su ID, devolviendo un DTO */
    HabitacionDto obtenerPorId(String id);
    
    /* Método para actualizar una habitación existente a partir de un DTO, devolviendo el DTO actualizado */
    HabitacionDto actualizar(String id, HabitacionDto dto);
    
    /* Método para cambiar el estado de una habitación, devolviendo el DTO actualizado */
    HabitacionDto cambiarEstado(String id, String nuevoEstado);
    
    /* Método para eliminar una habitación por su ID */
    void eliminar(String id);

    Page<HabitacionDto> listarTodasPaginadas(Pageable pageable);
}