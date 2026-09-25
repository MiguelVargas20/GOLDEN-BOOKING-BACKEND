package com.sena.goldenbooking.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sena.goldenbooking.auth.model.TipoToken;
import com.sena.goldenbooking.auth.service.RefreshTokenService;
import com.sena.goldenbooking.auth.service.TokenService;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.usuarios.dto.UsuarioRegistroDto;
import com.sena.goldenbooking.usuarios.mapper.UsuarioMapper;
import com.sena.goldenbooking.usuarios.model.Documento;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

/** Pruebas unitarias (sin Spring ni Mongo) del orden y el rollback del registro. */
class UsuarioServiceImplTest {

    private UsuarioRepository userRepo;
    private UsuarioAuthRepository authRepo;
    private EmailService emailService;
    private TokenService tokenService;
    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepo = mock(UsuarioRepository.class);
        authRepo = mock(UsuarioAuthRepository.class);
        emailService = mock(EmailService.class);
        tokenService = mock(TokenService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hash");
        service = new UsuarioServiceImpl(userRepo, authRepo, mock(UsuarioMapper.class),
                encoder, emailService, tokenService, mock(RefreshTokenService.class));

        when(userRepo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId("u1");
            return u;
        });
        when(tokenService.generarToken(anyString(), any())).thenReturn("token");
    }

    private UsuarioRegistroDto dto() {
        return UsuarioRegistroDto.builder()
                .nombre("Ana").apellido("Paz")
                .documento(new Documento("CC", "123"))
                .email("ana@test.com").username("anapaz").password("12345678")
                .build();
    }

    @Test
    void enviaElCorreoSoloDespuesDeGuardarLasCredenciales() {
        service.registrarUsuario(dto());

        InOrder orden = inOrder(authRepo, emailService);
        orden.verify(authRepo).save(any(UsuarioAuth.class));
        orden.verify(emailService).enviarCorreoVerificacion("ana@test.com", "token");
    }

    @Test
    void siFallanLasCredencialesSeBorraElPerfilYNoSeEnviaCorreo() {
        when(authRepo.save(any(UsuarioAuth.class))).thenThrow(new RuntimeException("Mongo caído"));

        assertThrows(RuntimeException.class, () -> service.registrarUsuario(dto()));

        verify(userRepo).deleteById("u1");
        verify(tokenService, never()).generarToken(anyString(), any(TipoToken.class));
        verify(emailService, never()).enviarCorreoVerificacion(anyString(), anyString());
    }
}
