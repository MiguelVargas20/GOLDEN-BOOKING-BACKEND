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

    public UsuarioServiceImpl(UsuarioRepository userRepo, 
                              UsuarioAuthRepository authRepo, 
                              UsuarioMapper userMapper, 
                              PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * REGISTRO MAESTRO: Crea Perfil y Auth sincronizados por ID.
     */
    @Override
    @Transactional // Recomendado para asegurar que se creen ambos o ninguno
    public UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto dto) {
        
        // 1. Validaciones previas
        if (userRepo.existsByDocNum(dto.getDocumento().getNumeroD())) {
            throw new RuntimeException("El documento ya está registrado.");
        }
        if (authRepo.existsByUser(dto.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya está en uso.");
        }

        // 2. Crear y guardar el Perfil (Colección: Usuario)
        Usuario perfil = Usuario.builder()
                .nomUsr(dto.getNombre())
                .apellUsr(dto.getApellido())
                .docId(dto.getDocumento())
                .tel(dto.getTelefono())
                .correo(dto.getEmail())
                .dir(dto.getDireccion())
                .fNac(dto.getFechaNacimiento())
                .estado(dto.getEstado())
                .fReg(LocalDateTime.now()) // Fecha automática de registro
                .build();

        // Al guardar, MongoDB genera el ID
        Usuario perfilGuardado = userRepo.save(perfil);

        // 3. Crear y guardar las Credenciales (Colección: UsuarioAuth)
        UsuarioAuth auth = new UsuarioAuth();
        auth.setId(perfilGuardado.getId()); // ¡CLAVE!: Sincronización de IDs
        auth.setUser(dto.getUsername());
        auth.setPwd(passwordEncoder.encode(dto.getPassword())); // Encriptación BCrypt
        auth.setRls(List.of(Rol.ROL_CLIENTE)); // Rol por defecto

        authRepo.save(auth);

        return dto;
    }

    // --- MÉTODOS DE LECTURA ---

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

    // --- ACTUALIZACIÓN Y ELIMINACIÓN ---

    @Override
    public UsuarioDto actualizarUsuario(String id, UsuarioDto usuarioDto) {
        Usuario usuarioExistente = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe usuario con ID: " + id));

        // Actualizamos campos de perfil usando el mapper que ya creamos
        userMapper.actualizarUsuario(usuarioDto, usuarioExistente);

        return userMapper.toDto(userRepo.save(usuarioExistente));
    }

    @Override
    @Transactional
    public void eliminarUsuario(String id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("ID no encontrado para eliminar.");
        }
        
        // Eliminamos de ambas colecciones para mantener la integridad
        userRepo.deleteById(id);
        if (authRepo.existsById(id)) {
            authRepo.deleteById(id);
        }
    }
}