package com.sena.goldenbooking.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sena.goldenbooking.auth.repository.TokenInvalidadoRepository;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.Rol;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

/** Pérdida de acceso inmediata: el estado y los roles se leen de la BD en cada petición. */
class JwtFilterTest {

    private JwtService jwtService;
    private UsuarioAuthRepository authRepo;
    private UsuarioRepository usuarioRepo;
    private JwtFilter filtro;
    private String token;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "CLAVE", "clave-de-pruebas-de-al-menos-32-caracteres!!");
        authRepo = mock(UsuarioAuthRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        filtro = new JwtFilter(jwtService, mock(TokenInvalidadoRepository.class), authRepo, usuarioRepo);

        // El token dice ROL_ADMIN a propósito: el filtro debe ignorarlo y usar la BD
        token = jwtService.generarToken("ana", List.of("ROL_ADMIN"), "Ana", "Paz", "u1");

        UsuarioAuth auth = new UsuarioAuth();
        auth.setId("u1");
        auth.setUser("ana");
        auth.setRls(List.of(Rol.ROL_CLIENTE));
        when(authRepo.findByUser("ana")).thenReturn(Optional.of(auth));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest ejecutar() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/reservas/hotel/mis-reservas");
        req.addHeader("Authorization", "Bearer " + token);
        filtro.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return req;
    }

    private void usuarioConEstado(EstadoUsuario estado) {
        Usuario perfil = Usuario.builder().id("u1").estado(estado).build();
        when(usuarioRepo.findById("u1")).thenReturn(Optional.of(perfil));
    }

    @Test
    void usuarioActivoSeAutenticaConLosRolesDeLaBaseDeDatos() throws Exception {
        usuarioConEstado(EstadoUsuario.ACTIVO);

        ejecutar();

        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacion);
        assertEquals(List.of("ROL_CLIENTE"),
                autenticacion.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    @Test
    void usuarioDesactivadoPierdeElAccesoAunqueSuTokenSigaVigente() throws Exception {
        usuarioConEstado(EstadoUsuario.INACTIVO);

        MockHttpServletRequest req = ejecutar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(JwtFilter.MOTIVO_CUENTA_INACTIVA, req.getAttribute(JwtFilter.ATRIBUTO_MOTIVO));
    }

    @Test
    void usuarioEliminadoPierdeElAcceso() throws Exception {
        when(usuarioRepo.findById("u1")).thenReturn(Optional.empty());

        MockHttpServletRequest req = ejecutar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(JwtFilter.MOTIVO_CUENTA_INACTIVA, req.getAttribute(JwtFilter.ATRIBUTO_MOTIVO));
    }

    @Test
    void tokenAlteradoNoAutentica() throws Exception {
        token = token.substring(0, token.length() - 3) + "abc";

        MockHttpServletRequest req = ejecutar();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(JwtFilter.MOTIVO_TOKEN_INVALIDO, req.getAttribute(JwtFilter.ATRIBUTO_MOTIVO));
    }
}
