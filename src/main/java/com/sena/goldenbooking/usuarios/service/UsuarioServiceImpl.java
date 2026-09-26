package com.sena.goldenbooking.usuarios.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sena.goldenbooking.auth.model.TipoToken;
import com.sena.goldenbooking.auth.service.RefreshTokenService;
import com.sena.goldenbooking.auth.service.TokenService;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.reservas.service.ReservasPorDocumentoService;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.dto.UsuarioRegistroDto;
import com.sena.goldenbooking.usuarios.mapper.UsuarioMapper;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.Rol;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository userRepo;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    private static final String MENSAJE_REGISTRO_NO_POSIBLE =
            "No fue posible completar el registro con los datos ingresados. "
            + "Si ya tienes una cuenta, inicia sesión o recupera tu contraseña.";

    private final ReservasPorDocumentoService reservasPorDocumento;

    public UsuarioServiceImpl(UsuarioRepository userRepo, UsuarioAuthRepository authRepo,
            UsuarioMapper userMapper, PasswordEncoder passwordEncoder,
            EmailService emailService, TokenService tokenService,
            RefreshTokenService refreshTokenService,
            ReservasPorDocumentoService reservasPorDocumento) {
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.reservasPorDocumento = reservasPorDocumento;
    }

    @Override
    @Transactional
    public UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto dto) {
        log.info("Iniciando registro de usuario: {}", dto.getUsername());

        // Validaciones previas.
        //
        // Documento y correo responden el MISMO mensaje genérico: antes decían
        // "El documento ya está registrado" / "Este correo ya está registrado",
        // lo que permitía a cualquiera averiguar si una persona (por su cédula
        // o su correo) tiene cuenta en el sistema. El detalle queda solo en el log.
        // El nombre de usuario sí se indica, porque lo elige la persona y
        // necesita saber que debe escoger otro.
        if (userRepo.existsByDocNum(dto.getDocumento().getNumeroD())) {
            log.warn("Registro rechazado: documento {} ya registrado.", dto.getDocumento().getNumeroD());
            throw new ConflictoDeNegocioException(MENSAJE_REGISTRO_NO_POSIBLE);
        }
        if (authRepo.existsByUser(dto.getUsername())) {
            log.warn("Registro rechazado: username '{}' ya en uso.", dto.getUsername());
            throw new ConflictoDeNegocioException("Ese nombre de usuario no está disponible. Por favor elige otro.");
        }
        // FIX hallazgo #9: faltaba este chequeo. Usuario.correo tiene índice único en
        // Mongo, así que sin esta validación un segundo registro con el mismo correo
        // (pero documento/username distintos) pasaba las dos validaciones de arriba y
        // reventaba recién al guardar con un DuplicateKeyException no mapeado — el
        // usuario veía "Ocurrió un error interno en el servidor" (500) en vez de un
        // mensaje claro.
        if (userRepo.existsByCorreo(dto.getEmail())) {
            log.warn("Registro rechazado: correo '{}' ya registrado.", dto.getEmail());
            throw new ConflictoDeNegocioException(MENSAJE_REGISTRO_NO_POSIBLE);
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
        // SEGURIDAD: el registro público SIEMPRE asigna ROL_CLIENTE, sin excepción.
        // Antes este valor se leía de dto.getRoles() (controlado por quien llama al
        // endpoint), lo que permitía que cualquiera se autoasignara ROL_ADMIN en el
        // body del registro. El DTO ya no tiene ese campo; esto es la segunda barrera.
        auth.setRls(List.of(Rol.ROL_CLIENTE));

        // @Transactional no tiene efecto aquí (no hay MongoTransactionManager y
        // Mongo sin replica set no admite transacciones), así que si falla el
        // guardado de credenciales se deshace a mano el perfil: antes quedaba un
        // perfil huérfano con ese correo/documento que bloqueaba volver a
        // registrarse ("El documento ya está registrado").
        try {
            authRepo.save(auth);
        } catch (RuntimeException e) {
            log.error("Falló el guardado de credenciales de '{}'; se elimina el perfil creado.", dto.getUsername(), e);
            userRepo.deleteById(perfilGuardado.getId());
            throw e;
        }

        // 3. Correo de verificación AL FINAL, cuando el usuario ya existe completo.
        //    Antes se enviaba antes de guardar las credenciales: si ese guardado
        //    fallaba, el usuario recibía un enlace para una cuenta que no existía.
        //    No bloquea el registro si falla el envío (EmailService es @Async).
        try {
            String token = tokenService.generarToken(perfilGuardado.getCorreo(), TipoToken.VERIFICACION_CUENTA);
            emailService.enviarCorreoVerificacion(perfilGuardado.getCorreo(), token);
        } catch (Exception e) {
            log.warn("No se pudo generar/enviar el correo de verificación a {}: {}", perfilGuardado.getCorreo(), e.getMessage());
        }

        log.info("Usuario '{}' registrado correctamente con ID: {}", dto.getUsername(), perfilGuardado.getId());
        return dto;
    }

    @Override
    public List<UsuarioDto> listarUsuarios() {
        List<UsuarioDto> usuarios = userRepo.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        log.info("Listado de usuarios solicitado. Total: {}", usuarios.size());
        return usuarios;
    }

    @Override
    public UsuarioDto obtenerPorId(String id) {
        return userRepo.findById(id)
                .map(u -> {
                    log.info("Usuario encontrado con ID: {}", id);
                    return userMapper.toDto(u);
                })
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con ID: {}", id);
                    return new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id);
                });
    }

    @Override
    public UsuarioDto obtenerPorDocNum(String docnum) {
        return userRepo.findByDocNum(docnum)
                .map(userMapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con documento: {}", docnum);
                    return new RecursoNoEncontradoException("Usuario no encontrado con documento: " + docnum);
                });
    }

    @Override
    public boolean existePorDocumento(String docnum) {
        return userRepo.existsByDocNum(docnum);
    }

    @Override
    public UsuarioDto actualizarUsuario(String id, UsuarioDto dto, String usernameSolicitante) {
        log.info("Actualizando usuario con ID: {}", id);
        Usuario usuario = userRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actualización fallida: usuario con ID {} no encontrado.", id);
                    return new RecursoNoEncontradoException("No existe usuario con ID: " + id);
                });
        UsuarioAuth auth = authRepo.findById(id).orElse(null);

        validarEdicion(dto);
        boolean esElMismoAdmin = auth != null && auth.getUser() != null && auth.getUser().equals(usernameSolicitante);
        if (esElMismoAdmin && dto.getEstado() == EstadoUsuario.INACTIVO) {
            throw new ConflictoDeNegocioException("No puedes desactivar tu propia cuenta.");
        }
        if (esElMismoAdmin && dto.getRoles() != null && !dto.getRoles().contains(Rol.ROL_ADMIN)) {
            throw new ConflictoDeNegocioException("No puedes quitarte tu propio rol de administrador.");
        }

        // Correo: único (índice único en Mongo). Aquí sí se dice el motivo: lo ve el admin.
        String correoNuevo = dto.getEmail() != null ? dto.getEmail().trim() : null;
        if (correoNuevo != null && !correoNuevo.equalsIgnoreCase(usuario.getCorreo()) && userRepo.existsByCorreo(correoNuevo)) {
            throw new ConflictoDeNegocioException("Ese correo ya pertenece a otro usuario.");
        }

        // Documento: único, y si cambia el número las reservas se trasladan al nuevo
        String docAnterior = usuario.getDocId() != null ? usuario.getDocId().getNumeroD() : null;
        String docNuevo = dto.getDocumento() != null && dto.getDocumento().getNumeroD() != null
                ? dto.getDocumento().getNumeroD().trim() : null;
        boolean cambiaDocumento = docNuevo != null && !docNuevo.equals(docAnterior);
        if (cambiaDocumento && userRepo.existsByDocNum(docNuevo)) {
            throw new ConflictoDeNegocioException("Ese número de documento ya pertenece a otro usuario.");
        }
        if (dto.getDocumento() != null && docNuevo != null) {
            dto.getDocumento().setNumeroD(docNuevo);
        }
        if (correoNuevo != null) dto.setEmail(correoNuevo);

        userMapper.actualizarUsuario(dto, usuario);
        Usuario guardado = userRepo.save(usuario);

        if (cambiaDocumento && docAnterior != null) {
            reservasPorDocumento.trasladarDocumento(docAnterior, docNuevo);
        }

        // Roles (viven en UsuarioAuth). JwtFilter los lee de la BD en cada
        // petición, así que el cambio aplica de inmediato.
        if (dto.getRoles() != null && auth != null) {
            auth.setRls(List.copyOf(new java.util.LinkedHashSet<>(dto.getRoles())));
            authRepo.save(auth);
            log.info("Roles del usuario ID {} actualizados a {}.", id, auth.getRls());
        }

        // Si el admin lo desactivó, se cierran sus sesiones: ya no podrá renovar
        // el token. (Sus peticiones con el access token actual también se
        // rechazan de inmediato: JwtFilter verifica el estado en cada petición.)
        if (guardado.getEstado() == EstadoUsuario.INACTIVO) {
            refreshTokenService.revocarTodosDelUsuario(id);
            log.info("Usuario ID {} desactivado: sesiones revocadas.", id);
        }
        log.info("Usuario con ID: {} actualizado correctamente.", id);
        return conRoles(userMapper.toDto(guardado), auth);
    }

    /** Reglas básicas de los campos que manda el formulario del admin. */
    private static void validarEdicion(UsuarioDto dto) {
        if (dto.getNombre() != null && dto.getNombre().isBlank()) {
            throw new SolicitudInvalidaException("El nombre no puede quedar vacío.");
        }
        if (dto.getApellido() != null && dto.getApellido().isBlank()) {
            throw new SolicitudInvalidaException("El apellido no puede quedar vacío.");
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new SolicitudInvalidaException("El correo no tiene un formato válido.");
        }
        if (dto.getDocumento() != null) {
            String numero = dto.getDocumento().getNumeroD();
            if (numero == null || !numero.trim().matches("^\\S{5,15}$")) {
                throw new SolicitudInvalidaException("El número de documento debe tener entre 5 y 15 caracteres, sin espacios.");
            }
        }
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()
                && !dto.getTelefono().trim().matches("^\\+?[0-9 ]{7,15}$")) {
            throw new SolicitudInvalidaException("El teléfono debe tener entre 7 y 15 dígitos.");
        }
        if (dto.getFechaNacimiento() != null && dto.getFechaNacimiento().isAfter(java.time.LocalDate.now())) {
            throw new SolicitudInvalidaException("La fecha de nacimiento no puede ser futura.");
        }
        if (dto.getRoles() != null && dto.getRoles().isEmpty()) {
            throw new SolicitudInvalidaException("El usuario debe tener al menos un rol.");
        }
    }

    private static UsuarioDto conRoles(UsuarioDto dto, UsuarioAuth auth) {
        if (dto != null && auth != null) dto.setRoles(auth.getRls());
        return dto;
    }

    @Override
    @Transactional
    public void eliminarUsuario(String id) {
        log.info("Iniciando eliminación de usuario con ID: {}", id);
        if (!userRepo.existsById(id)) {
            log.warn("Eliminación fallida: usuario con ID {} no encontrado.", id);
            throw new RecursoNoEncontradoException("ID no encontrado para eliminar.");
        }
        userRepo.deleteById(id);
        if (authRepo.existsById(id)) {
            authRepo.deleteById(id);
        }
        // Cierra todas sus sesiones abiertas
        refreshTokenService.revocarTodosDelUsuario(id);
        log.info("Usuario con ID: {} eliminado correctamente de perfil y credenciales.", id);
    }

    // Paginación
    @Override
    public Page<UsuarioDto> listarUsuariosPaginados(Pageable pageable) {
        log.info("Listado paginado de usuarios. Página: {}, Tamaño: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        Page<Usuario> pagina = userRepo.findAll(pageable);
        // Roles de toda la página en una sola consulta (para mostrarlos y editarlos)
        Map<String, UsuarioAuth> auths = new java.util.HashMap<>();
        authRepo.findAllById(pagina.map(Usuario::getId).getContent()).forEach(a -> auths.put(a.getId(), a));
        return pagina.map(u -> conRoles(userMapper.toDto(u), auths.get(u.getId())));
    }

    // Actualiza el perfil del usuario con los campos proporcionados en el mapa.
    @Override
    public UsuarioDto actualizarPerfil(String id, Map<String, String> campos) {
        log.info("Usuario actualizando su propio perfil. ID: {}", id);
        Usuario usuario = userRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Perfil no encontrado con ID: {}", id);
                    return new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id);
                });

        // Solo permite cambiar nombre, apellido, teléfono y correo
        if (campos.containsKey("nombre"))   usuario.setNomUsr(campos.get("nombre"));
        if (campos.containsKey("apellido")) usuario.setApellUsr(campos.get("apellido"));
        if (campos.containsKey("telefono")) usuario.setTel(campos.get("telefono"));
        if (campos.containsKey("correo"))   usuario.setCorreo(campos.get("correo"));

        UsuarioDto resultado = userMapper.toDto(userRepo.save(usuario));
        log.info("Perfil del usuario ID: {} actualizado correctamente.", id);
        return resultado;
    }

    // Resuelve el número de documento del usuario a partir de su username (subject del JWT)
    @Override
    public String obtenerDocumentoPorUsername(String username) {
        UsuarioAuth auth = authRepo.findByUser(username)
                .orElseThrow(() -> {
                    log.warn("No se encontró UsuarioAuth para username: {}", username);
                    return new RecursoNoEncontradoException("Usuario autenticado no encontrado.");
                });

        Usuario perfil = userRepo.findById(auth.getId())
                .orElseThrow(() -> {
                    log.warn("No se encontró perfil de Usuario para id: {}", auth.getId());
                    return new RecursoNoEncontradoException("Perfil de usuario no encontrado.");
                });

        return perfil.getDocId() != null ? perfil.getDocId().getNumeroD() : null;
    }

    // UsuarioAuth y Usuario (UsuarioPerfil) se registran con el mismo ID
    // (ver registrarUsuario), así que basta con leer el ID desde UsuarioAuth
    // sin necesidad de ir también a buscar el perfil completo.
    @Override
    public String obtenerIdPorUsername(String username) {
        UsuarioAuth auth = authRepo.findByUser(username)
                .orElseThrow(() -> {
                    log.warn("No se encontró UsuarioAuth para username: {}", username);
                    return new RecursoNoEncontradoException("Usuario autenticado no encontrado.");
                });
        return auth.getId();
    }

    // FIX hallazgo #11 (N+1 en RecordatorioService): una sola consulta $in en vez
    // de una por cada docNum. Se descartan usuarios sin docId para no reventar el
    // toMap con una clave nula si algún registro viejo llegara incompleto.
    @Override
    public Map<String, UsuarioDto> obtenerMapaPorDocNums(List<String> docNums) {
        if (docNums == null || docNums.isEmpty()) return Map.of();
        return userRepo.findByDocNumIn(docNums).stream()
                .filter(u -> u.getDocId() != null && u.getDocId().getNumeroD() != null)
                .collect(Collectors.toMap(
                        u -> u.getDocId().getNumeroD(),
                        userMapper::toDto,
                        (existente, duplicado) -> existente));
    }
}