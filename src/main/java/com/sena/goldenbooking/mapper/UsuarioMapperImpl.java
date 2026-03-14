package com.sena.goldenbooking.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.models.Usuario;

@Component
public class UsuarioMapperImpl implements UsuarioMapper {

    @Override
    public Usuario toUsuario(UsuarioDto usuarioDto) {
        if (usuarioDto == null)
            return null;

        return Usuario.builder()
                .id(usuarioDto.getId())
                .nom(usuarioDto.getNombre())
                .ape(usuarioDto.getApellido())
                .email(usuarioDto.getEmail())
                .doc(usuarioDto.getDocumento())
                .build();

        // Convierte un Dto en una Entidad Usuario
        // Se usa builder para inicializar el objeto de forma clara y segura
    }

    @Override
    public UsuarioDto toDto(Usuario usuario) {
        if (usuario == null)
            return null;

        return UsuarioDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getNom())
                .apellido(usuario.getApe())
                .documento(usuario.getDoc())
                .email(usuario.getEmail())
                .build();

        // Convierte una entidad Usuario(modelo) a Dto
        // Builder permite mapear campo por campo sin depender del orden del constructor
    }

    @Override
    public List<UsuarioDto> toDtoList(List<Usuario> usuarios) {
        if (usuarios == null)
            return null;

        return usuarios.stream()
                .map(this::toDto)
                .toList();
        // Convierte una lista de entidades Uusario en una lista de Dtos
        // Metodo stream + map para aplicar el metodo "toUsuarioDto" a cada elemento
    }

    @Override
    public void actualizarUsuario(UsuarioDto usuarioDto, Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario modelo no puede estar vacio");
        }
        if (usuarioDto == null) {
            throw new IllegalArgumentException("Usuario Dto no puede estar vacio");
        }
        // Si ambos son validos, se puede actualizar los campos de la entidad con datos
        // del Dto
        usuario.setNom(usuarioDto.getNombre());
        usuario.setApe((usuarioDto.getApellido()));
        usuario.setEmail(usuarioDto.getEmail());
        usuario.setDoc(usuarioDto.getDocumento());

        // Actualiza una entidad Usuario existente con los datos de un Dto
        // No se usa builder debido a que el objeto ya esta creado, solo se actualiza
        // campos con setters
    }

}
