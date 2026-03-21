package com.sena.goldenbooking.services;

import java.util.List;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;

public interface UsuarioService {
    //Crear usuario con Dto
    UsuarioDto crearUsuario(UsuarioDto usuarioDto);

    //Listar Usuarios segun Dto
    List<UsuarioDto> ListUsuarios();

    //Actualizar usuario
    UsuarioDto actualizarUsuario(String id, UsuarioDto usuarioDto);

    //Eliminar usuario
    void delete(String docnum);

    UsuarioDto UsuarioByDocNum(String docnum);

    UsuarioDto UsuarioByDocum(String docnum);

    UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto dto);

    UsuarioDto create(UsuarioDto usuarioDto);

    UsuarioDto update(String id, UsuarioDto usuarioDto);
}
