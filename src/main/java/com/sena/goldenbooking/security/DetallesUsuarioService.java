package com.sena.goldenbooking.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;

@Service
public class DetallesUsuarioService implements UserDetailsService {

    private final UsuarioAuthRepository authRepo;

    public DetallesUsuarioService(UsuarioAuthRepository authRepo) {
        this.authRepo = authRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsuarioAuth usuario = authRepo.findByUser(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.builder()
                .username(usuario.getUser())
                .password(usuario.getPwd())       // tu campo es pwd no pass
                .authorities(
                    usuario.getRls().stream()     // tu campo es rls no roles
                        .map(Enum::name)
                        .toArray(String[]::new))
                .build();
    }
}