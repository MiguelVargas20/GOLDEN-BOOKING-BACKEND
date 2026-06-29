package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;
import com.sena.goldenbooking.mapper.UsuarioMapper;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.models.Rol;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository userRepo;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository userRepo, UsuarioAuthRepository authRepo, UsuarioMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto dto) {
        log.info("Iniciando registro de usuario: {}", dto.getUsername());

        // Validaciones previas
        if (userRepo.existsByDocNum(dto.getDocumento().getNumeroD())) {
            log.warn("Registro rechazado: documento {} ya registrado.", dto.getDocumento().getNumeroD());
            throw new RuntimeException("El documento ya está registrado.");
        }
        if (authRepo.existsByUser(dto.getUsername())) {
            log.warn("Registro rechazado: username '{}' ya en uso.", dto.getUsername());
            throw new RuntimeException("El nombre de usuario ya está en uso.");
        }

        // 1. Guardar perfil en colección UsuarioPerfil
        Usuario perfil = Usuario.builder()
                .nomUsr(dto.getNombre())
                .apellUsr(dto.getApellido())
                .docId(dto.getDocumento())
                .tel(dto.getTelefono())
                .correo(dto.getEmail())
                .dir(dto.getDireccion())
                .fNac(dto.getFechaNacimiento())
                .estado(dto.getEstado())
                .fReg(LocalDateTime.now())
                .build();

        Usuario perfilGuardado = userRepo.save(perfil);

        // 2. Guardar credenciales en colección UsuarioAuth con el mismo ID
        UsuarioAuth auth = new UsuarioAuth();
        auth.setId(perfilGuardado.getId());
        auth.setUser(dto.getUsername());
        auth.setPwd(passwordEncoder.encode(dto.getPassword()));
        // Si el DTO trae roles los usamos, si no, asignamos ROL_CLIENTE por defecto
        auth.setRls(
            (dto.getRoles() != null && !dto.getRoles().isEmpty())
                ? dto.getRoles()
                : List.of(Rol.ROL_CLIENTE)
        );

        authRepo.save(auth);

        log.info("Usuario '{}' registrado correctamente con ID: {}", dto.getUsername(), perfilGuardado.getId());
        return dto;
    }

    @Override
    public List<UsuarioDto> listarUsuarios() {
        List<UsuarioDto> usuarios = userRepo.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        log.info("Listado de usuarios solicitado. Total: {}", usuarios.size());
        return usuarios;
    }

    @Override
    public UsuarioDto obtenerPorId(String id) {
        return userRepo.findById(id)
                .map(u -> {
                    log.info("Usuario encontrado con ID: {}", id);
                    return userMapper.toDto(u);
                })
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con ID: {}", id);
                    return new RuntimeException("Usuario no encontrado con ID: " + id);
                });
    }

    @Override
    public UsuarioDto obtenerPorDocNum(String docnum) {
        return userRepo.findByDocNum(docnum)
                .map(userMapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con documento: {}", docnum);
                    return new RuntimeException("Usuario no encontrado con documento: " + docnum);
                });
    }

    @Override
    public boolean existePorDocumento(String docnum) {
        return userRepo.existsByDocNum(docnum);
    }

    @Override
    public UsuarioDto actualizarUsuario(String id, UsuarioDto usuarioDto) {
        log.info("Actualizando usuario con ID: {}", id);
        Usuario usuarioExistente = userRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actualización fallida: usuario con ID {} no encontrado.", id);
                    return new RuntimeException("No existe usuario con ID: " + id);
                });

        userMapper.actualizarUsuario(usuarioDto, usuarioExistente);
        UsuarioDto resultado = userMapper.toDto(userRepo.save(usuarioExistente));
        log.info("Usuario con ID: {} actualizado correctamente.", id);
        return resultado;
    }

    @Override
    @Transactional
    public void eliminarUsuario(String id) {
        log.info("Iniciando eliminación de usuario con ID: {}", id);
        if (!userRepo.existsById(id)) {
            log.warn("Eliminación fallida: usuario con ID {} no encontrado.", id);
            throw new RuntimeException("ID no encontrado para eliminar.");
        }
        userRepo.deleteById(id);
        if (authRepo.existsById(id)) {
            authRepo.deleteById(id);
        }
        log.info("Usuario con ID: {} eliminado correctamente de perfil y credenciales.", id);
    }

    // Paginación
    @Override
    public Page<UsuarioDto> listarUsuariosPaginados(Pageable pageable) {
        log.info("Listado paginado de usuarios. Página: {}, Tamaño: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        return userRepo.findAll(pageable).map(userMapper::toDto);
    }
}