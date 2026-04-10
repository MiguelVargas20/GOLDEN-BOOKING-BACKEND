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

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository userRepo;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository userRepo,UsuarioAuthRepository authRepo,UsuarioMapper userMapper,PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto dto) {

        // Validaciones previas
        if (userRepo.existsByDocNum(dto.getDocumento().getNumeroD())) {
            throw new RuntimeException("El documento ya está registrado.");
        }
        if (authRepo.existsByUser(dto.getUsername())) {
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

        return dto;
    }

    @Override
    public List<UsuarioDto> listarUsuarios() {
        return userRepo.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDto obtenerPorId(String id) {
        Usuario usuario = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return userMapper.toDto(usuario);
    }

    @Override
    public UsuarioDto obtenerPorDocNum(String docnum) {
        Usuario usuario = userRepo.findByDocNum(docnum)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con documento: " + docnum));
        return userMapper.toDto(usuario);
    }

    @Override
    public boolean existePorDocumento(String docnum) {
        return userRepo.existsByDocNum(docnum);
    }

    @Override
    public UsuarioDto actualizarUsuario(String id, UsuarioDto usuarioDto) {
        Usuario usuarioExistente = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe usuario con ID: " + id));

        userMapper.actualizarUsuario(usuarioDto, usuarioExistente);

        return userMapper.toDto(userRepo.save(usuarioExistente));
    }

    @Override
    @Transactional
    public void eliminarUsuario(String id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("ID no encontrado para eliminar.");
        }
        userRepo.deleteById(id);
        if (authRepo.existsById(id)) {
            authRepo.deleteById(id);
        }
    }
}