package com.sena.goldenbooking.habitaciones.service;

import java.util.List;

import com.sena.goldenbooking.habitaciones.dto.TipoHabitacionDto;

public interface TipoHabitacionService {
    // Define los métodos del servicio para manejar las operaciones CRUD
    TipoHabitacionDto crear(TipoHabitacionDto dto);

    // Lista todos los tipos de habitación
    List<TipoHabitacionDto> listarTodos();

    // Obtiene un tipo de habitación por su ID
    TipoHabitacionDto obtenerPorId(String id);

    // Actualiza un tipo de habitación existente
    TipoHabitacionDto actualizar(String id, TipoHabitacionDto dto);

    // Elimina un tipo de habitación por su ID
    void eliminar(String id);
}