package com.sena.goldenbooking.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.TipoHabitacionDto;
import com.sena.goldenbooking.mapper.TipoHabitacionMapper;
import com.sena.goldenbooking.models.TipoHabitacion;
import com.sena.goldenbooking.repositories.TipoHabitacionRepository;


// Implementación del servicio para manejar las operaciones CRUD de TipoHabitacion
@Service
public class TipoHabitacionServiceImpl implements TipoHabitacionService {

    // Inyección de dependencias del repositorio y el mapper
    private final TipoHabitacionRepository repo;
    private final TipoHabitacionMapper mapper;

    // Constructor para inyectar las dependencias
    public TipoHabitacionServiceImpl(TipoHabitacionRepository repo, TipoHabitacionMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }


    // Implementación de los métodos del servicio utilizando el repositorio y el mapper
    @Override
    public TipoHabitacionDto crear(TipoHabitacionDto dto) {
        TipoHabitacion tipo = mapper.toTipoHabitacion(dto);
        return mapper.toDto(repo.save(tipo));
    }


    // Método para listar todos los tipos de habitación, mapeando las entidades a DTOs
    @Override
    public List<TipoHabitacionDto> listarTodos() {
        return mapper.toDtoList(repo.findAll());
    }


    // Método para obtener un tipo de habitación por su ID, lanzando una excepción si no se encuentra
    @Override
    public TipoHabitacionDto obtenerPorId(String id) {
        TipoHabitacion tipo = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de habitación no encontrado: " + id));
        return mapper.toDto(tipo);
    }

    // Método para actualizar un tipo de habitación existente, lanzando una excepción si no se encuentra
    @Override
    public TipoHabitacionDto actualizar(String id, TipoHabitacionDto dto) {
        TipoHabitacion tipo = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de habitación no encontrado: " + id));
        mapper.actualizar(dto, tipo);
        return mapper.toDto(repo.save(tipo));
    }

    // Método para eliminar un tipo de habitación por su ID, lanzando una excepción si no se encuentra
    @Override
    public void eliminar(String id) {
        if (!repo.existsById(id)) throw new RuntimeException("No existe el tipo: " + id);
        repo.deleteById(id);
    }
}