package com.sena.goldenbooking.services;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.dtos.LoginResponsiveDto;
import com.sena.goldenbooking.models.*;
import com.sena.goldenbooking.repositories.*;
import com.sena.goldenbooking.security.JwtService;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;                          // ← faltaba declarar
    private final TokenInvalidadoRepository tokenInvalidadoRepo;

    public AuthServiceImpl(
            UsuarioAuthRepository authRepo,
            UsuarioRepository userRepo,                           // ← era usuarioRepo
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenInvalidadoRepository tokenInvalidadoRepo) {
        this.authRepo = authRepo;
        this.userRepo = userRepo;                                 // ← era this.usuarioRepo
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;                            // ← ya funciona
        this.tokenInvalidadoRepo = tokenInvalidadoRepo;
    }

    @Override
    public LoginResponsiveDto login(LoginDto dto) {
        String identificador = dto.getUsername();
        UsuarioAuth auth;

        Optional<UsuarioAuth> porUser = authRepo.findByUser(identificador);

        if (porUser.isPresent()) {
            auth = porUser.get();
        } else {
            Usuario usuario = userRepo.findByDocNum(identificador)
                .or(() -> userRepo.findAll().stream()
                            .filter(u -> u.getCorreo().equals(identificador))
                            .findFirst())
                .orElseThrow(() -> new RuntimeException("Credenciales no encontradas"));

            auth = authRepo.findById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Error de integridad: Perfil sin credenciales"));
        }

        if (!passwordEncoder.matches(dto.getPassword(), auth.getPwd())) {
            throw new RuntimeException("Contraseña o usuario incorrectos");
        }

        Usuario usuarioPerfil = userRepo.findById(auth.getId())
            .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        return LoginResponsiveDto.builder()
            .token(null)
            .id(usuarioPerfil.getId())
            .username(auth.getUser())
            .email(usuarioPerfil.getCorreo())
            .roles(auth.getRls())
            .build();
    }

    @Override
    public void logout(String token) {
        TokenInvalidado tokenInvalidado = TokenInvalidado.builder()
                .token(token)
                .expiracion(jwtService.extraerExpiracion(token))
                .build();
        tokenInvalidadoRepo.save(tokenInvalidado);
    }
}