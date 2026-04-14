package com.sena.goldenbooking.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.models.EstadoUsuario;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;

@Service
public class DetallesUsuarioService implements UserDetailsService {

    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository usuarioRepo; // ← agregar

    public DetallesUsuarioService(UsuarioAuthRepository authRepo, UsuarioRepository usuarioRepo) {
        this.authRepo = authRepo;
        this.usuarioRepo = usuarioRepo; // ← agregar
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Buscar credenciales
        UsuarioAuth auth = authRepo.findByUser(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // 2. Buscar perfil para validar estado
        Usuario perfil = usuarioRepo.findById(auth.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Perfil no encontrado: " + username));

        // 3. Validar que el usuario esté ACTIVO ← esto es lo nuevo
        if (perfil.getEstado() == EstadoUsuario.INACTIVO) {
            throw new UsernameNotFoundException("Usuario inactivo. Contacte al administrador.");
        }

        // 4. Construir UserDetails
        return User.builder()
                .username(auth.getUser())
                .password(auth.getPwd())
                .authorities(
                    auth.getRls().stream()
                        .map(Enum::name)
                        .toArray(String[]::new))
                .build();
    }
}