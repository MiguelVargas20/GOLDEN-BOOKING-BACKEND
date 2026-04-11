package com.sena.goldenbooking.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import com.sena.goldenbooking.dtos.UsuarioAuthDto;
import com.sena.goldenbooking.models.UsuarioAuth;

@Component
public class UsuarioAuthMapperImpl implements UsuarioAuthMapper {

    /* Convertir DTO a modelo (Entidad para MongoDB) */
    @Override
    public UsuarioAuth toEntity(UsuarioAuthDto dto) {
        if (dto == null) return null;

        UsuarioAuth auth = new UsuarioAuth();
        auth.setId(dto.getId());
        auth.setUser(dto.getUsuario());
        
        // Pasamos la contraseña tal cual (el Service se encargará de encriptarla)
        auth.setPwd(dto.getPassword());
        auth.setRls(dto.getRoles());
        
        return auth;
    }

    /* Convertir modelo a DTO (Respuesta para el cliente) */
    @Override
    public UsuarioAuthDto toDto(UsuarioAuth entity) {
        if (entity == null) return null;

        UsuarioAuthDto dto = new UsuarioAuthDto();
        dto.setId(entity.getId());
        dto.setUsuario(entity.getUser());

        // Por seguridad, no seteamos el password en el DTO de salida
        // Aunque Jackson lo bloquee, es buena práctica dejarlo null aquí
        dto.setPassword(null); 
        dto.setRoles(entity.getRls());
        
        return dto;
    }

    /* Convertir una lista de modelos a una lista de DTOs */
    @Override
    public List<UsuarioAuthDto> toDtoList(List<UsuarioAuth> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toDto).toList();
    }

    /* Actualizar una entidad con los datos del DTO */
    @Override
    public void actualizarAuth(UsuarioAuthDto dto, UsuarioAuth entity) {
        if (dto == null || entity == null) return;

        entity.setUser(dto.getUsuario());

        
        // Solo actualizamos la contraseña si el DTO trae una nueva
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            entity.setPwd(dto.getPassword());
        }
        
        // Actualizamos los roles si el DTO los trae (puede ser una actualización de permisos)
        entity.setRls(dto.getRoles());
    }
}