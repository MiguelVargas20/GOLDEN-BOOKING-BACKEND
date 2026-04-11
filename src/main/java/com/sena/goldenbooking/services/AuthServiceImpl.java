package com.sena.goldenbooking.services;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.dtos.LoginResponsiveDto;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UsuarioAuthRepository authRepo, 
                           UsuarioRepository userRepo, 
                           PasswordEncoder passwordEncoder) {
        this.authRepo = authRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponsiveDto login(LoginDto dto) {
        // En tu LoginDto el campo es 'email', pero el usuario puede meter su doc o su username
        String identificador = dto.getEmail(); 
        UsuarioAuth auth;

        // 1. Intentar buscar por Username (Colección UsuarioAuth)
        Optional<UsuarioAuth> porUser = authRepo.findByUser(identificador);
        
        if (porUser.isPresent()){
            auth = porUser.get();
        } else {
            // 2. Si no es username, buscar en Perfil (Colección Usuario) por correo o documento
            // Usamos los métodos que ya corregimos en tu UsuarioRepository
            Usuario usuario = userRepo.findByDocNum(identificador)
                .or(() -> userRepo.findAll().stream() // Búsqueda manual por correo si no tienes el query
                            .filter(u -> u.getCorreo().equals(identificador))
                            .findFirst())
                .orElseThrow(() -> new RuntimeException("Credenciales no encontradas"));

            // Como los IDs están sincronizados, buscamos el Auth con el ID del Perfil
            auth = authRepo.findById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Error de integridad: Perfil sin credenciales"));
        }

        // 3. Verificar contraseña (BCrypt)
        // Ojo: En tu modelo UsuarioAuth el campo es 'pwd'
        if (!passwordEncoder.matches(dto.getPassword(), auth.getPwd())) {
            throw new RuntimeException("Contraseña o usuario incorrectos");
        }

        // 4. Buscar el Perfil completo para la respuesta
        Usuario usuarioPerfil = userRepo.findById(auth.getId())
            .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        // 5. Construir respuesta (LoginResponseDto / LoginResponsiveDto según tu código)
        return LoginResponsiveDto.builder()
            .token(null) // El token se generará cuando implementemos JWT
            .id(usuarioPerfil.getId())
            .username(auth.getUser())
            .email(usuarioPerfil.getCorreo())
            .roles(auth.getRls()) // Atributo 'rls' de tu modelo
            .build();
    }
}