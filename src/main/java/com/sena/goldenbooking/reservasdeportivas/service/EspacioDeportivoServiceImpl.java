package com.sena.goldenbooking.reservasdeportivas.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.dto.EspacioDeportivoDto;
import com.sena.goldenbooking.reservasdeportivas.mapper.EspacioDeportivoMapper;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;
import com.sena.goldenbooking.reservasdeportivas.repository.EspacioDeportivoRepository;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EspacioDeportivoServiceImpl implements EspacioDeportivoService {

    /** Tamaño máximo de imagen (también limitado en spring.servlet.multipart.*). */
    static final long TAMANIO_MAXIMO_IMAGEN = 5L * 1024 * 1024;

    /** Dimensiones mínimas: por debajo la imagen se ve pixelada en las tarjetas del catálogo. */
    static final int ANCHO_MINIMO = 400;
    static final int ALTO_MINIMO = 300;

    /**
     * Dimensiones máximas: una imagen muy comprimida puede pesar poco y ocupar
     * gigas de memoria al abrirse ("bomba de descompresión").
     */
    static final int LADO_MAXIMO = 8000;

    private static final Sort ORDEN_CATALOGO = Sort.by("deporte").ascending().and(Sort.by("nombre").ascending());

    private final EspacioDeportivoRepository repo;
    private final ReservaDeporteRepository reservaDeporteRepo;
    private final EspacioDeportivoMapper mapper;
    private final GridFsTemplate gridFs;

    public EspacioDeportivoServiceImpl(EspacioDeportivoRepository repo,
                                       ReservaDeporteRepository reservaDeporteRepo,
                                       EspacioDeportivoMapper mapper,
                                       GridFsTemplate gridFs) {
        this.repo = repo;
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.mapper = mapper;
        this.gridFs = gridFs;
    }

    // ── Consultas ──────────────────────────────────────────────────────────

    @Override
    public List<EspacioDeportivoDto> listar(boolean incluirInactivos) {
        List<EspacioDeportivo> espacios = incluirInactivos
                ? repo.findAll(ORDEN_CATALOGO)
                : repo.findByEstadoIn(EnumSet.of(EstadoEspacio.ACTIVO, EstadoEspacio.MANTENIMIENTO), ORDEN_CATALOGO);
        return mapper.toDtoList(espacios);
    }

    @Override
    public EspacioDeportivoDto obtenerPorId(String id) {
        return mapper.toDto(buscar(id));
    }

    @Override
    public EspacioDeportivo obtenerReservable(String id) {
        if (id == null || id.isBlank()) {
            throw new SolicitudInvalidaException("Debes seleccionar el espacio deportivo a reservar.");
        }
        EspacioDeportivo espacio = buscar(id);
        if (espacio.getEstado() == EstadoEspacio.MANTENIMIENTO) {
            throw new ConflictoDeNegocioException("Este espacio está en mantenimiento y no se puede reservar por ahora.");
        }
        if (espacio.getEstado() != EstadoEspacio.ACTIVO) {
            throw new ConflictoDeNegocioException("Este espacio no está disponible para reservas.");
        }
        return espacio;
    }

    // ── Administración ─────────────────────────────────────────────────────

    @Override
    public EspacioDeportivoDto crear(EspacioDeportivoDto dto) {
        validarHorario(dto);
        EspacioDeportivo espacio = mapper.toEntity(dto);
        if (repo.existsByNombreIgnoreCase(espacio.getNombre())) {
            throw new ConflictoDeNegocioException("Ya existe un espacio deportivo con ese nombre.");
        }
        if (espacio.getEstado() == null) {
            espacio.setEstado(EstadoEspacio.ACTIVO);
        }
        espacio.setFechaCreacion(ZonaHoraria.ahora());
        espacio.setFechaActualizacion(espacio.getFechaCreacion());

        EspacioDeportivo guardado = repo.save(espacio);
        log.info("Espacio deportivo creado: {} ({})", guardado.getNombre(), guardado.getId());
        return mapper.toDto(guardado);
    }

    @Override
    public EspacioDeportivoDto actualizar(String id, EspacioDeportivoDto dto) {
        validarHorario(dto);
        EspacioDeportivo espacio = buscar(id);
        String nombreNuevo = dto.getNombre() == null ? null : dto.getNombre().trim().replaceAll("\\s+", " ");
        if (repo.existsByNombreIgnoreCaseAndIdNot(nombreNuevo, id)) {
            throw new ConflictoDeNegocioException("Ya existe otro espacio deportivo con ese nombre.");
        }
        mapper.actualizar(dto, espacio);
        espacio.setFechaActualizacion(ZonaHoraria.ahora());

        log.info("Espacio deportivo actualizado: {} ({})", espacio.getNombre(), id);
        return mapper.toDto(repo.save(espacio));
    }

    @Override
    public EspacioDeportivoDto cambiarEstado(String id, EstadoEspacio estado) {
        if (estado == null) {
            throw new SolicitudInvalidaException("Debes indicar el nuevo estado del espacio.");
        }
        EspacioDeportivo espacio = buscar(id);
        espacio.setEstado(estado);
        espacio.setFechaActualizacion(ZonaHoraria.ahora());
        log.info("Espacio deportivo {} cambió a estado {}", id, estado);
        // Las reservas ya existentes no se tocan: el admin decide si las cancela
        // desde la gestión de reservas (así puede avisar a cada cliente con un motivo).
        return mapper.toDto(repo.save(espacio));
    }

    @Override
    public void eliminar(String id) {
        EspacioDeportivo espacio = buscar(id);

        if (reservaDeporteRepo.existsByEspacioIdAndEstadoNotAndFechaFinReservaAfter(
                id, EstadoReserva.CANCELADA, ZonaHoraria.ahora())) {
            throw new ConflictoDeNegocioException(
                    "No se puede eliminar: el espacio tiene reservas vigentes. Cancélalas primero o márcalo como INACTIVO.");
        }
        if (reservaDeporteRepo.existsByEspacioId(id)) {
            // Borrarlo dejaría reservas pasadas apuntando a un espacio inexistente
            throw new ConflictoDeNegocioException(
                    "No se puede eliminar: el espacio tiene historial de reservas. Márcalo como INACTIVO para ocultarlo.");
        }

        borrarArchivoImagen(espacio.getImagenId());
        repo.delete(espacio);
        log.info("Espacio deportivo eliminado: {} ({})", espacio.getNombre(), id);
    }

    // ── Imágenes (GridFS) ──────────────────────────────────────────────────

    @Override
    public EspacioDeportivoDto subirImagen(String id, MultipartFile archivo) {
        EspacioDeportivo espacio = buscar(id);

        if (archivo == null || archivo.isEmpty()) {
            throw new SolicitudInvalidaException("Debes seleccionar una imagen.");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO_IMAGEN) {
            throw new SolicitudInvalidaException("La imagen no puede superar 5 MB.");
        }

        String tipo;
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
            // El tipo se detecta por los primeros bytes del archivo, no por la
            // extensión ni por el Content-Type que manda el navegador (ambos se
            // pueden falsificar para subir, por ejemplo, un HTML o un ejecutable).
            tipo = detectarTipoImagen(contenido);
        } catch (IOException e) {
            throw new SolicitudInvalidaException("No se pudo leer la imagen. Intenta con otro archivo.");
        }
        if (tipo == null) {
            throw new SolicitudInvalidaException("Formato no permitido. Sube una imagen JPG, PNG o WEBP.");
        }
        validarDimensiones(contenido, tipo);

        ObjectId nuevoId = gridFs.store(new ByteArrayInputStream(contenido),
                "espacio-" + id, tipo);
        String anterior = espacio.getImagenId();

        espacio.setImagenId(nuevoId.toHexString());
        espacio.setFechaActualizacion(ZonaHoraria.ahora());
        EspacioDeportivo guardado = repo.save(espacio);

        borrarArchivoImagen(anterior); // se borra la vieja solo cuando la nueva ya quedó guardada
        log.info("Imagen actualizada para el espacio {} ({} bytes, {})", id, contenido.length, tipo);
        return mapper.toDto(guardado);
    }

    @Override
    public EspacioDeportivoDto eliminarImagen(String id) {
        EspacioDeportivo espacio = buscar(id);
        borrarArchivoImagen(espacio.getImagenId());
        espacio.setImagenId(null);
        espacio.setFechaActualizacion(ZonaHoraria.ahora());
        return mapper.toDto(repo.save(espacio));
    }

    @Override
    public GridFsResource obtenerImagen(String id) {
        EspacioDeportivo espacio = buscar(id);
        if (espacio.getImagenId() == null) {
            throw new RecursoNoEncontradoException("Este espacio no tiene imagen propia.");
        }
        GridFSFile archivo = gridFs.findOne(porId(espacio.getImagenId()));
        if (archivo == null) {
            throw new RecursoNoEncontradoException("No se encontró la imagen del espacio.");
        }
        return gridFs.getResource(archivo);
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private EspacioDeportivo buscar(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El espacio deportivo no existe."));
    }

    private void validarHorario(EspacioDeportivoDto dto) {
        if (dto.getHoraApertura() != null && dto.getHoraCierre() != null
                && !dto.getHoraCierre().isAfter(dto.getHoraApertura())) {
            throw new SolicitudInvalidaException("La hora de cierre debe ser posterior a la hora de apertura.");
        }
    }

    private void borrarArchivoImagen(String imagenId) {
        if (imagenId != null) {
            gridFs.delete(porId(imagenId));
        }
    }

    private static Query porId(String imagenId) {
        return Query.query(Criteria.where("_id").is(new ObjectId(imagenId)));
    }

    /**
     * Valida ancho y alto leyendo SOLO la cabecera del archivo (sin decodificar
     * la imagen completa en memoria). Si la cabecera no se puede leer, el
     * archivo está dañado o no es realmente una imagen.
     */
    private void validarDimensiones(byte[] contenido, String tipo) {
        int[] dimensiones = leerDimensiones(contenido, tipo);
        if (dimensiones == null) {
            throw new SolicitudInvalidaException("La imagen está dañada o no se pudo leer. Intenta con otro archivo.");
        }
        int ancho = dimensiones[0];
        int alto = dimensiones[1];
        if (ancho < ANCHO_MINIMO || alto < ALTO_MINIMO) {
            throw new SolicitudInvalidaException("La imagen es muy pequeña (" + ancho + "×" + alto
                    + " px). El mínimo es " + ANCHO_MINIMO + "×" + ALTO_MINIMO + " px.");
        }
        if (ancho > LADO_MAXIMO || alto > LADO_MAXIMO) {
            throw new SolicitudInvalidaException("La imagen es demasiado grande (" + ancho + "×" + alto
                    + " px). El máximo es " + LADO_MAXIMO + " px por lado.");
        }
    }

    /** {ancho, alto} de la imagen, o null si la cabecera no es válida. */
    static int[] leerDimensiones(byte[] b, String tipo) {
        if ("image/webp".equals(tipo)) {
            return leerDimensionesWebp(b);
        }
        // JPG y PNG: ImageIO lee el tamaño desde la cabecera sin decodificar los píxeles
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(b))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) return null;
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                return new int[] { lector.getWidth(0), lector.getHeight(0) };
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Ancho y alto de un WEBP según su primer bloque (el JDK no trae lector de WEBP):
     * VP8 (con pérdida), VP8L (sin pérdida) o VP8X (extendido, con transparencia/animación).
     */
    private static int[] leerDimensionesWebp(byte[] b) {
        if (b.length < 30) return null;
        String bloque = new String(b, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        switch (bloque) {
            case "VP8 " -> {
                // Firma de fotograma clave 9D 01 2A y luego ancho/alto de 14 bits
                if ((b[23] & 0xFF) != 0x9D || (b[24] & 0xFF) != 0x01 || (b[25] & 0xFF) != 0x2A) return null;
                int ancho = ((b[26] & 0xFF) | (b[27] & 0xFF) << 8) & 0x3FFF;
                int alto = ((b[28] & 0xFF) | (b[29] & 0xFF) << 8) & 0x3FFF;
                return new int[] { ancho, alto };
            }
            case "VP8L" -> {
                if ((b[20] & 0xFF) != 0x2F) return null;
                int bits = (b[21] & 0xFF) | (b[22] & 0xFF) << 8 | (b[23] & 0xFF) << 16 | (b[24] & 0xFF) << 24;
                return new int[] { (bits & 0x3FFF) + 1, ((bits >> 14) & 0x3FFF) + 1 };
            }
            case "VP8X" -> {
                int ancho = ((b[24] & 0xFF) | (b[25] & 0xFF) << 8 | (b[26] & 0xFF) << 16) + 1;
                int alto = ((b[27] & 0xFF) | (b[28] & 0xFF) << 8 | (b[29] & 0xFF) << 16) + 1;
                return new int[] { ancho, alto };
            }
            default -> {
                return null;
            }
        }
    }

    /** Devuelve el MIME type según la "firma" (magic bytes) del archivo, o null si no es JPG/PNG/WEBP. */
    static String detectarTipoImagen(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
        if (b.length >= 8 && Arrays.equals(Arrays.copyOf(b, 8), png)) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
