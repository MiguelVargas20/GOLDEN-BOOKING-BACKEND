package com.sena.goldenbooking.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sena.goldenbooking.auth.service.RefreshTokenService;
import com.sena.goldenbooking.auth.service.TokenService;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reservas.service.ReservasPorDocumentoService;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.mapper.UsuarioMapperImpl;
import com.sena.goldenbooking.usuarios.model.Documento;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.Rol;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

/** Edición completa de un usuario por el ADMIN. */
class EdicionUsuarioAdminTest {

    private UsuarioRepository userRepo;
    private UsuarioAuthRepository authRepo;
    private ReservasPorDocumentoService reservasPorDocumento;
    private UsuarioServiceImpl service;
    private Usuario usuario;
    private UsuarioAuth auth;

    @BeforeEach
    void setUp() {
        userRepo = mock(UsuarioRepository.class);
        authRepo = mock(UsuarioAuthRepository.class);
        reservasPorDocumento = mock(ReservasPorDocumentoService.class);
        service = new UsuarioServiceImpl(userRepo, authRepo, new UsuarioMapperImpl(), mock(PasswordEncoder.class),
                mock(EmailService.class), mock(TokenService.class), mock(RefreshTokenService.class), reservasPorDocumento);

        Documento doc = new Documento();
        doc.setTipo("CC");
        doc.setNumeroD("100000");
        usuario = Usuario.builder().id("u1").nomUsr("Ana").apellUsr("Gómez").correo("ana@test.com")
                .docId(doc).estado(EstadoUsuario.ACTIVO).build();
        auth = new UsuarioAuth();
        auth.setId("u1");
        auth.setUser("ana");
        auth.setRls(List.of(Rol.ROL_CLIENTE));

        when(userRepo.findById("u1")).thenReturn(Optional.of(usuario));
        when(authRepo.findById("u1")).thenReturn(Optional.of(auth));
        when(userRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Documento documento(String numero) {
        Documento d = new Documento();
        d.setTipo("CC");
        d.setNumeroD(numero);
        return d;
    }

    @Test
    void cambiarElDocumentoTrasladaLasReservas() {
        UsuarioDto cambios = UsuarioDto.builder().documento(documento(" 200000 ")).build();

        UsuarioDto r = service.actualizarUsuario("u1", cambios, "admin");

        assertEquals("200000", r.getDocumento().getNumeroD());
        verify(reservasPorDocumento).trasladarDocumento("100000", "200000");
    }

    @Test
    void documentoRepetidoSeRechazaSinTocarReservas() {
        when(userRepo.existsByDocNum("200000")).thenReturn(true);
        assertThrows(ConflictoDeNegocioException.class,
                () -> service.actualizarUsuario("u1", UsuarioDto.builder().documento(documento("200000")).build(), "admin"));
        verify(reservasPorDocumento, never()).trasladarDocumento(anyString(), anyString());
    }

    @Test
    void correoDeOtroUsuarioSeRechaza() {
        when(userRepo.existsByCorreo("otro@test.com")).thenReturn(true);
        assertThrows(ConflictoDeNegocioException.class,
                () -> service.actualizarUsuario("u1", UsuarioDto.builder().email("otro@test.com").build(), "admin"));
    }

    @Test
    void elAdminPuedeCambiarElRol() {
        UsuarioDto r = service.actualizarUsuario("u1",
                UsuarioDto.builder().roles(List.of(Rol.ROL_ADMIN, Rol.ROL_ADMIN)).build(), "admin");
        assertEquals(List.of(Rol.ROL_ADMIN), r.getRoles());
        verify(authRepo).save(auth);
    }

    @Test
    void unAdminNoSeQuitaSuRolNiSeDesactiva() {
        auth.setRls(List.of(Rol.ROL_ADMIN));
        assertThrows(ConflictoDeNegocioException.class,
                () -> service.actualizarUsuario("u1", UsuarioDto.builder().roles(List.of(Rol.ROL_CLIENTE)).build(), "ana"));
        assertThrows(ConflictoDeNegocioException.class,
                () -> service.actualizarUsuario("u1", UsuarioDto.builder().estado(EstadoUsuario.INACTIVO).build(), "ana"));
    }

    @Test
    void validaLosCampos() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.actualizarUsuario("u1", UsuarioDto.builder().email("no-es-correo").build(), "admin"));
        assertThrows(SolicitudInvalidaException.class,
                () -> service.actualizarUsuario("u1", UsuarioDto.builder().telefono("12ab").build(), "admin"));
        assertThrows(SolicitudInvalidaException.class,
                () -> service.actualizarUsuario("u1", UsuarioDto.builder().nombre("  ").build(), "admin"));
        assertThrows(SolicitudInvalidaException.class, () -> service.actualizarUsuario("u1",
                UsuarioDto.builder().fechaNacimiento(java.time.LocalDate.now().plusDays(1)).build(), "admin"));
    }
}
