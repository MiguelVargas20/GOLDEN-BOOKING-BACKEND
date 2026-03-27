package com.sena.goldenbooking.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;
import com.sena.goldenbooking.mapper.UsuarioMapper;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.repositories.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository userRepo;
    private final UsuarioMapper userMapper;

    public UsuarioServiceImpl(UsuarioRepository userRepo, UsuarioMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.userMapper = userMapper;
    }

    // --- MÉTODOS DE CREACIÓN ---

    @Override
    public UsuarioDto crearUsuario(UsuarioDto usuarioDto) {
        // VALIDACIÓN: Evitar duplicados por documento antes de crear

        // Asumiendo que el DTO tiene el número de documento accesible

        if (userRepo.findByDocNum(usuarioDto.getDocumento().getNumero()).isPresent()) {
            throw new RuntimeException("El documento ya se encuentra registrado");
        }

        Usuario usuario = userMapper.toUsuario(usuarioDto);
        if (usuario.getId() == null || usuario.getId().isEmpty()) {
            usuario.setId(UUID.randomUUID().toString());
        }
        return userMapper.toDto(userRepo.save(usuario));
    }

    // --- MÉTODOS DE LECTURA ---

    @Override
    public List<UsuarioDto> ListUsuarios() {
        return userRepo.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDto UsuarioByDocNum(String docnum) {
        Usuario usuario = userRepo.findByDocNum(docnum)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con documento: " + docnum));
        return userMapper.toDto(usuario);
    }

    @Override
    public UsuarioDto UsuarioByDocum(String docnum) {
        return UsuarioByDocNum(docnum);
    }

    // --- MÉTODO DE ACTUALIZACIÓN CORREGIDO (POR DOCUMENTO) ---

    @Override
    public UsuarioDto actualizarUsuarios(String docnum, UsuarioDto usuarioDto) {
        // 1. Buscamos al usuario por el número de documento (usando el campo de tu
        // modelo)
        Usuario usuarioExistente = userRepo.findByDocNum(docnum)
                .orElseThrow(() -> new RuntimeException(
                        "No se puede actualizar, no existe usuario con documento: " + docnum));

        // 2. Actualizamos los campos usando el Mapper
        userMapper.actualizarUsuario(usuarioDto, usuarioExistente);

        // 3. Guardamos los cambios en MongoDB
        Usuario usuarioActualizado = userRepo.save(usuarioExistente);

        // 4. Retornamos el DTO
        return userMapper.toDto(usuarioActualizado);
    }
// --- MÉTODO DE REGISTRO ---

    @Override
    public UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto dto) {
        // 1. Validación de duplicados por número de documento
        if (userRepo.findByDocNum(dto.getDocumento().getNumero()).isPresent()) {
            throw new RuntimeException("Error: El documento " + dto.getDocumento().getNumero() + " ya está registrado.");
        }

        // 2. Mapeo manual de DTO de Registro a Modelo Usuario (usando Builder)
        Usuario nuevoUsuario = Usuario.builder()
                .id(UUID.randomUUID().toString())
                .nom(dto.getNombre())
                .ape(dto.getApellido())
                .doc(dto.getDocumento())
                .email(dto.getEmail())
                // Si tu DTO de registro tiene password, aquí lo encriptas:
                // .password(passwordEncoder.encode(dto.getPassword()))
                .build();

        // 3. Persistencia en MongoDB
        userRepo.save(nuevoUsuario);

        // Si usas el passwordEncoder aquí (aunque sea comentando la lógica de negocio), 
        // la advertencia del IDE debería desaparecer.
        return dto;
    }
    // --- MÉTODOS DE ELIMINACIÓN ---

    @Override
    public void delete(String id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("No se puede eliminar, ID no encontrado: " + id);
        }
        userRepo.deleteById(id);
    }
}