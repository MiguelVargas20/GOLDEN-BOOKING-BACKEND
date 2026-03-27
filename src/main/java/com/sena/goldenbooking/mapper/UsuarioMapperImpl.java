package com.sena.goldenbooking.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.models.Usuario;

@Component
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public Usuario toUsuario(UsuarioDto usuarioDto) {
        if (usuarioDto == null) {
            return null;
        }

        return Usuario.builder()
                .id(usuarioDto.getId())
                .nom(usuarioDto.getNombre())
                .ape(usuarioDto.getApellido())
                .doc(usuarioDto.getDocumento())
                .email(usuarioDto.getEmail())
                // Agrega aquí otros campos como password, telefono, etc.
                .build();
    }

    @Override
    public UsuarioDto toDto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return UsuarioDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getNom())
                .apellido(usuario.getApe())
                .documento(usuario.getDoc())
                .email(usuario.getEmail())
                .build();
    }

    @Override
    public List<UsuarioDto> toDtoList(List<Usuario> usuarios) {
        if (usuarios == null) {
            return null;
        }
        return usuarios.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void actualizarUsuario(UsuarioDto usuarioDto, Usuario usuario) {
        if (usuario == null || usuarioDto == null) {
            throw new IllegalArgumentException("El usuario o el DTO no pueden ser nulos");
        }

        // Actualización de campos manual
        usuario.setNom(usuarioDto.getNombre());
        usuario.setApe(usuarioDto.getApellido());
        usuario.setDoc(usuarioDto.getDocumento());
        usuario.setEmail(usuarioDto.getEmail());
        // No solemos actualizar el ID ni la contraseña aquí por seguridad
    }
}