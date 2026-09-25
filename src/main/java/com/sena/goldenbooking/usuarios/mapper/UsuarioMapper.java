package com.sena.goldenbooking.usuarios.mapper;

import java.util.List;

import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.model.Usuario;

public interface UsuarioMapper {
    Usuario toUsuario(UsuarioDto usuarioDto); 

    UsuarioDto toDto(Usuario usuario); 

    List<UsuarioDto> toDtoList(List<Usuario> usuarios); 

    void actualizarUsuario(UsuarioDto usuarioDto, Usuario usuario);
}