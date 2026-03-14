package com.sena.goldenbooking.mapper;

import java.util.List;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.models.Usuario;

public interface UsuarioMapper {
    Usuario toUsuario(UsuarioDto usuarioDto); // Metodo para convertir de Dto a Usuario(modelo)

    UsuarioDto toDto(Usuario usuario); // Metodo para convertir Usuario(modelo) a Dto

    List<UsuarioDto> toDtoList(List<Usuario> usuarios); // Metodo para converitr Lista de Dtos a Lista de Usuari(Modelo)

    void actualizarUsuario(UsuarioDto usuarioDto, Usuario usuario); // Metodo para actualizar un usuario existente con
                                                                    // datos de UsuarioDto
}
