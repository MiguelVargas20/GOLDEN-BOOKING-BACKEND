package com.sena.goldenbooking.mapper;

import java.util.List;

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
                .nomUsr(usuarioDto.getNombre())
                .apellUsr(usuarioDto.getApellido())
                // Asumiendo que el DTO trae los objetos Documento y Direccion
                .docId(usuarioDto.getDocumento()) 
                .correo(usuarioDto.getEmail())
                .tel(usuarioDto.getTelefono())
                .fNac(usuarioDto.getFechaNacimiento())
                .dir(usuarioDto.getDireccion())
                .estado(usuarioDto.getEstado())
                .fReg(usuarioDto.getFechaRegistro())
                .build();
    }

    @Override
    public UsuarioDto toDto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return UsuarioDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getNomUsr())
                .apellido(usuario.getApellUsr())
                .documento(usuario.getDocId())
                .email(usuario.getCorreo())
                .telefono(usuario.getTel())
                .direccion(usuario.getDir())
                .fechaNacimiento(usuario.getFNac())
                .estado(usuario.getEstado())
                .fechaRegistro(usuario.getFReg())
                .build();
    }

    @Override
    public List<UsuarioDto> toDtoList(List<Usuario> usuarios) {
        if (usuarios == null) {
            return null;
        }
        return usuarios.stream()
                .map(this::toDto)
                .toList(); // En Java 16+ es más limpio que collect(Collectors.toList())
    }

    @Override
    public void actualizarUsuario(UsuarioDto usuarioDto, Usuario usuario) {
        if (usuario == null || usuarioDto == null) {
            throw new IllegalArgumentException("El usuario o el DTO no pueden ser nulos");
        }

        // Actualización PARCIAL: solo se copian los campos que vienen en el DTO.
        // Antes se copiaban todos, y el formulario de edición del admin
        // (UsuariosE.jsx) solo manda nombre, apellido, email y estado — así que
        // cada edición dejaba en null el documento, teléfono, dirección y fecha
        // de nacimiento. Sin documento el usuario ya no podía reservar y perdía
        // el vínculo con sus reservas (que se buscan por número de documento).
        if (usuarioDto.getNombre() != null)          usuario.setNomUsr(usuarioDto.getNombre());
        if (usuarioDto.getApellido() != null)        usuario.setApellUsr(usuarioDto.getApellido());
        if (usuarioDto.getEmail() != null)           usuario.setCorreo(usuarioDto.getEmail());
        if (usuarioDto.getTelefono() != null)        usuario.setTel(usuarioDto.getTelefono());
        if (usuarioDto.getDireccion() != null)       usuario.setDir(usuarioDto.getDireccion());
        if (usuarioDto.getDocumento() != null)       usuario.setDocId(usuarioDto.getDocumento());
        if (usuarioDto.getFechaNacimiento() != null) usuario.setFNac(usuarioDto.getFechaNacimiento());
        if (usuarioDto.getEstado() != null)          usuario.setEstado(usuarioDto.getEstado());
        
        // El ID y la fecha de registro (fReg) normalmente no se actualizan
    }
}