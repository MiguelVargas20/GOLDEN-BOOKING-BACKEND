package com.sena.goldenbooking.mapper;

import java.util.List;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.models.Usuario;

public interface UsuarioMapper {
    Usuario toUsuario(UsuarioDto usuarioDto); 

    UsuarioDto toDto(Usuario usuario); 

    List<UsuarioDto> toDtoList(List<Usuario> usuarios); 

    void actualizarUsuario(UsuarioDto usuarioDto, Usuario usuario);
}