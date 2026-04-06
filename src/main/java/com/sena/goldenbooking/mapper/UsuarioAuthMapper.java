package com.sena.goldenbooking.mapper;

import java.util.List;
import com.sena.goldenbooking.dtos.UsuarioAuthDto;
import com.sena.goldenbooking.models.UsuarioAuth;

public interface UsuarioAuthMapper {

    /* Convertir DTO a modelo (Entidad para MongoDB) */
    UsuarioAuth toEntity(UsuarioAuthDto dto);
    
    /* Convertir modelo a DTO (Respuesta para el cliente) */
    UsuarioAuthDto toDto(UsuarioAuth entity);
    
    /* Convertir una lista de modelos a una lista de DTOs */
    List<UsuarioAuthDto> toDtoList(List<UsuarioAuth> entities);
    
    /* Actualizar una entidad con los datos del DTO */
    void actualizarAuth(UsuarioAuthDto dto, UsuarioAuth entity);
}