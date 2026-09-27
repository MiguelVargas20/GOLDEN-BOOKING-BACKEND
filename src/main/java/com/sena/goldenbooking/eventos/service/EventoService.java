package com.sena.goldenbooking.eventos.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.compartido.imagenes.AlmacenImagenes;
import com.sena.goldenbooking.eventos.dto.EventoDto;
import com.sena.goldenbooking.eventos.model.Evento;
import com.sena.goldenbooking.eventos.repository.EventoRepository;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.Rol;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Eventos del club (bailes, festivales, recreativos...). El admin los crea y
 * al publicarlos aparecen arriba en la portada del cliente y se le avisa en su
 * campana.
 */
@Slf4j
@Service
public class EventoService {

    /** Días durante los que un evento recién creado se marca como "Nuevo". */
    static final int DIAS_NUEVO = 7;
    private static final String RUTA_IMAGEN = "/api/eventos/%s/imagen?v=%s";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM, h:mm a", Locale.forLanguageTag("es-CO"));

    private final EventoRepository repo;
    private final AlmacenImagenes imagenes;
    private final NotificacionService notificaciones;
    private final UsuarioRepository usuarioRepo;
    private final UsuarioAuthRepository authRepo;

    public EventoService(EventoRepository repo, AlmacenImagenes imagenes, NotificacionService notificaciones,
                         UsuarioRepository usuarioRepo, UsuarioAuthRepository authRepo) {
        this.repo = repo;
        this.imagenes = imagenes;
        this.notificaciones = notificaciones;
        this.usuarioRepo = usuarioRepo;
        this.authRepo = authRepo;
    }

    /** Publicados que aún no terminan, el más próximo primero (portada del cliente). */
    public List<EventoDto> proximos() {
        return repo.findByPublicadoTrueAndFechaFinAfter(ZonaHoraria.ahora(), Sort.by("fechaInicio")).stream().map(this::aDto).toList();
    }

    /** Todos, los más recientes primero (panel del admin). */
    public List<EventoDto> listarTodos() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "fechaInicio")).stream().map(this::aDto).toList();
    }

    public EventoDto crear(EventoDto dto) {
        validarFechas(dto, true);
        LocalDateTime ahora = ZonaHoraria.ahora();
        Evento e = new Evento();
        copiar(dto, e);
        e.setFechaCreacion(ahora);
        e.setFechaActualizacion(ahora);
        Evento guardado = repo.save(e);
        if (guardado.isPublicado()) avisarClientes(guardado);
        return aDto(guardado);
    }

    public EventoDto actualizar(String id, EventoDto dto) {
        Evento e = buscar(id);
        validarFechas(dto, false);
        boolean seAcabaDePublicar = !e.isPublicado() && dto.isPublicado();
        copiar(dto, e);
        e.setFechaActualizacion(ZonaHoraria.ahora());
        Evento guardado = repo.save(e);
        if (seAcabaDePublicar) avisarClientes(guardado);
        return aDto(guardado);
    }

    public void eliminar(String id) {
        Evento e = buscar(id);
        repo.delete(e);
        imagenes.borrar(e.getImagenId());
    }

    public EventoDto subirImagen(String id, MultipartFile archivo) {
        Evento e = buscar(id);
        String nueva = imagenes.guardar(archivo, "evento-" + id);
        String anterior = e.getImagenId();
        e.setImagenId(nueva);
        Evento guardado = repo.save(e);
        imagenes.borrar(anterior);
        return aDto(guardado);
    }

    public EventoDto eliminarImagen(String id) {
        Evento e = buscar(id);
        imagenes.borrar(e.getImagenId());
        e.setImagenId(null);
        return aDto(repo.save(e));
    }

    public GridFsResource obtenerImagen(String id) {
        Evento e = buscar(id);
        if (e.getImagenId() == null) throw new RecursoNoEncontradoException("Este evento no tiene imagen propia.");
        return imagenes.obtener(e.getImagenId());
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private static void validarFechas(EventoDto dto, boolean nuevo) {
        if (!dto.getFechaFin().isAfter(dto.getFechaInicio())) {
            throw new SolicitudInvalidaException("La fecha de fin debe ser posterior a la de inicio.");
        }
        if (nuevo && dto.getFechaFin().isBefore(ZonaHoraria.ahora())) {
            throw new SolicitudInvalidaException("No se puede crear un evento que ya terminó.");
        }
    }

    private static void copiar(EventoDto dto, Evento e) {
        e.setTitulo(dto.getTitulo().trim());
        e.setDescripcion(dto.getDescripcion() != null && !dto.getDescripcion().isBlank() ? dto.getDescripcion().trim() : null);
        e.setCategoria(dto.getCategoria());
        e.setFechaInicio(dto.getFechaInicio());
        e.setFechaFin(dto.getFechaFin());
        e.setLugar(dto.getLugar().trim());
        e.setCupo(dto.getCupo());
        e.setPrecio(dto.getPrecio() != null && dto.getPrecio() > 0 ? dto.getPrecio() : null);
        e.setPublicado(dto.isPublicado());
    }

    /** Notificación "Nuevo evento" para todos los clientes activos. Si falla, el evento igual queda publicado. */
    private void avisarClientes(Evento e) {
        try {
            Set<String> admins = authRepo.findAll().stream()
                    .filter(a -> a.getRls() != null && a.getRls().contains(Rol.ROL_ADMIN))
                    .map(UsuarioAuth::getId).collect(Collectors.toSet());
            List<String> documentos = usuarioRepo.findAll().stream()
                    .filter(u -> !admins.contains(u.getId()) && u.getEstado() != EstadoUsuario.INACTIVO && u.getDocId() != null)
                    .map(u -> u.getDocId().getNumeroD())
                    .toList();
            String mensaje = e.getTitulo() + " · " + e.getFechaInicio().format(FECHA) + " · " + e.getLugar();
            documentos.forEach(doc -> notificaciones.notificar(doc, TipoNotificacion.EVENTO, null, e.getId(), "Nuevo evento", mensaje));
            log.info("Evento '{}' publicado: se avisó a {} clientes.", e.getTitulo(), documentos.size());
        } catch (Exception ex) {
            log.warn("No se pudo avisar a los clientes del evento {}: {}", e.getId(), ex.getMessage());
        }
    }

    private Evento buscar(String id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El evento no existe."));
    }

    private EventoDto aDto(Evento e) {
        return EventoDto.builder()
                .id(e.getId())
                .titulo(e.getTitulo())
                .descripcion(e.getDescripcion())
                .categoria(e.getCategoria())
                .fechaInicio(e.getFechaInicio())
                .fechaFin(e.getFechaFin())
                .lugar(e.getLugar())
                .cupo(e.getCupo())
                .precio(e.getPrecio())
                .publicado(e.isPublicado())
                .imagenUrl(e.getImagenId() != null ? RUTA_IMAGEN.formatted(e.getId(), e.getImagenId()) : null)
                .nuevo(e.getFechaCreacion() != null && e.getFechaCreacion().isAfter(ZonaHoraria.ahora().minusDays(DIAS_NUEVO)))
                .fechaCreacion(e.getFechaCreacion())
                .build();
    }
}
