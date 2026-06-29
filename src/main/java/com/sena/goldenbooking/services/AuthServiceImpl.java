package com.sena.goldenbooking.services;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j; // ← IMPORTANTE: Importar Slf4j de Lombok
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.dtos.LoginResponsiveDto;
import com.sena.goldenbooking.models.*;
import com.sena.goldenbooking.repositories.*;
import com.sena.goldenbooking.security.JwtService;

@Slf4j // ← PASO 1: Anotación para inyectar la variable "log"
@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;                          
    private final TokenInvalidadoRepository tokenInvalidadoRepo;

    public AuthServiceImpl(
            UsuarioAuthRepository authRepo,
            UsuarioRepository userRepo,                           
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenInvalidadoRepository tokenInvalidadoRepo) {
        this.authRepo = authRepo;
        this.userRepo = userRepo;                                 
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;                            
        this.tokenInvalidadoRepo = tokenInvalidadoRepo;
    }

    @Override
    public LoginResponsiveDto login(LoginDto dto) {
        String identificador = dto.getUsername();
        UsuarioAuth auth;

        log.info("Iniciando proceso de autenticación para el identificador: {}", identificador); // ← Trazabilidad inicial

        Optional<UsuarioAuth> porUser = authRepo.findByUser(identificador);

        if (porUser.isPresent()) {
            auth = porUser.get();
        } else {
            Usuario usuario = userRepo.findByDocNum(identificador)
                .or(() -> userRepo.findAll().stream()
                            .filter(u -> u.getCorreo().equals(identificador))
                            .findFirst())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido: No se encontró el usuario con identificador '{}'", identificador); // ← WARN de seguridad
                    return new RuntimeException("Credenciales no encontradas");
                });

            auth = authRepo.findById(usuario.getId())
                .orElseThrow(() -> {
                    log.error("Error de integridad de datos: El usuario '{}' existe pero no tiene credenciales (UsuarioAuth)", identificador); // ← ERROR crítico de base de datos
                    return new RuntimeException("Error de integridad: Perfil sin credenciales");
                });
        }

        if (!passwordEncoder.matches(dto.getPassword(), auth.getPwd())) {
            log.warn("Intento de login fallido: Contraseña incorrecta para el usuario '{}'", identificador); // ← WARN de seguridad
            throw new RuntimeException("Contraseña o usuario incorrectos");
        }

        Usuario usuarioPerfil = userRepo.findById(auth.getId())
            .orElseThrow(() -> {
                log.error("Error de integridad: Credenciales validadas pero perfil de usuario no encontrado para ID '{}'", auth.getId()); // ← ERROR crítico
                return new RuntimeException("Perfil no encontrado");
            });

        // ← INFO: Autenticación 100% exitosa
        log.info("Usuario '{}' (Email: {}) se ha autenticado correctamente", auth.getUser(), usuarioPerfil.getCorreo());

        return LoginResponsiveDto.builder()
            .token(null) // Nota: Asumo que el token lo generas o inyectas en otro lado más adelante
            .id(usuarioPerfil.getId())
            .username(auth.getUser())
            .email(usuarioPerfil.getCorreo())
            .roles(auth.getRls())
            .build();
    }

    @Override
    public void logout(String token) {
        try {
            TokenInvalidado tokenInvalidado = TokenInvalidado.builder()
                    .token(token)
                    .expiracion(jwtService.extraerExpiracion(token))
                    .build();
            tokenInvalidadoRepo.save(tokenInvalidado);
            
            // ← INFO: Logout exitoso
            log.info("Logout exitoso: Token invalidado correctamente hasta su expiración");
            
        } catch (Exception e) {
            log.error("Error al intentar invalidar el token durante el logout", e); // ← ERROR: Si la base de datos falla al guardar
            throw e;
        }
    }
}