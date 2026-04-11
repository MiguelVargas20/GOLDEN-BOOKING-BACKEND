package com.sena.goldenbooking.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.HabitacionDto;
import com.sena.goldenbooking.mapper.HabitacionMapper;
import com.sena.goldenbooking.models.Habitacion;
import com.sena.goldenbooking.repositories.HabitacionRepository;

@Service
public class HabitacionServiceImpl implements HabitacionService {

    // Inyectamos el repositorio y el mapper a través del constructor
    private final HabitacionRepository habRepo;
    private final HabitacionMapper habMapper;


    // Constructor para inyección de dependencias
    public HabitacionServiceImpl(HabitacionRepository habRepo, HabitacionMapper habMapper) {
        this.habRepo = habRepo;
        this.habMapper = habMapper;
    }

    /* Implementación de los métodos definidos en la interfaz HabitacionService */
    @Override
    public HabitacionDto crear(HabitacionDto dto) {

        // Convertimos DTO a Entidad
        Habitacion habitacion = habMapper.toHabitacion(dto);

        // Guardamos en MongoDB y retornamos el DTO convertido
        return habMapper.toDto(habRepo.save(habitacion));
    }

    /* Método para listar todas las habitaciones, devolviendo una lista de DTOs */

    @Override
    public List<HabitacionDto> listarTodas() {
        return habMapper.toDtoList(habRepo.findAll());
    }

    /* Método para listar habitaciones por estado, devolviendo una lista de DTOs */
    @Override
    public List<HabitacionDto> listarPorEstado(String estado) {
        // Usa findByEstado del repositorio
        return habMapper.toDtoList(habRepo.findByEstado(estado));
    }

    /* Método para listar habitaciones por tipo, devolviendo una lista de DTOs */
    @Override
    public List<HabitacionDto> listarPorTipo(String idTipoHabitacion) {
        // Usa el método de navegación NoSQL findByTipoHabitacion_Id
        return habMapper.toDtoList(habRepo.findByTipoHabitacion_Id(idTipoHabitacion));
    }

    /* Método para obtener una habitación por su ID, devolviendo un DTO */
    @Override
    public HabitacionDto obtenerPorId(String id) {
        Habitacion hab = habRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada con ID: " + id));
        return habMapper.toDto(hab);
    }

    /* Método para actualizar una habitación existente a partir de un DTO, devolviendo el DTO actualizado */
    @Override
    public HabitacionDto actualizar(String id, HabitacionDto dto) {
        Habitacion habExistente = habRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No se puede actualizar, ID no encontrado: " + id));
        
        // Usamos tu método de Mapper para pasar los datos del DTO a la entidad existente
        habMapper.actualizarHabitacion(dto, habExistente);
        
        return habMapper.toDto(habRepo.save(habExistente));
    }

    /* Método para cambiar el estado de una habitación, devolviendo el DTO actualizado */
    @Override
    public HabitacionDto cambiarEstado(String id, String nuevoEstado) {
        Habitacion hab = habRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada: " + id));
        
        hab.setEstado(nuevoEstado);
        return habMapper.toDto(habRepo.save(hab));
    }

    /* Método para eliminar una habitación por su ID */
    @Override
    public void eliminar(String id) {
        if (!habRepo.existsById(id)) {
            throw new RuntimeException("No se puede eliminar, ID no existe: " + id);
        }
        habRepo.deleteById(id);
    }
}