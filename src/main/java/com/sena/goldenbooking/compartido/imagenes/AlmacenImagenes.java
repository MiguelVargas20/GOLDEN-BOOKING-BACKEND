package com.sena.goldenbooking.compartido.imagenes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;

import lombok.extern.slf4j.Slf4j;

/**
 * Guarda, valida, entrega y borra imágenes en GridFS (MongoDB).
 * Lo usan los espacios deportivos y las habitaciones, con las mismas reglas:
 * JPG, PNG o WEBP detectados por su firma (no por la extensión), máximo 5 MB
 * y entre 400×300 y 8000×8000 px.
 */
@Slf4j
@Service
public class AlmacenImagenes {

    /** Máximo 5 MB (el límite de multipart en application.properties es 6 MB). */
    public static final long TAMANIO_MAXIMO_IMAGEN = 5L * 1024 * 1024;

    /** Por debajo de esto la imagen se ve pixelada en las tarjetas. */
    public static final int ANCHO_MINIMO = 400;
    public static final int ALTO_MINIMO = 300;

    /** Por encima de esto no aporta nada y es sospechoso (bombas de descompresión). */
    public static final int LADO_MAXIMO = 8000;

    private final GridFsTemplate gridFs;

    public AlmacenImagenes(GridFsTemplate gridFs) {
        this.gridFs = gridFs;
    }

    /**
     * Valida y guarda la imagen.
     * @param nombre nombre del archivo en GridFS (p. ej. "habitacion-123")
     * @return id del archivo guardado (hex)
     */
    public String guardar(MultipartFile archivo, String nombre) {
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

        ObjectId id = gridFs.store(new ByteArrayInputStream(contenido), nombre, tipo);
        log.info("Imagen guardada: {} ({} bytes, {})", nombre, contenido.length, tipo);
        return id.toHexString();
    }

    /** Borra la imagen si existe (null = no hace nada). */
    public void borrar(String imagenId) {
        if (imagenId != null) {
            gridFs.delete(porId(imagenId));
        }
    }

    /** El archivo para enviarlo al navegador. */
    public GridFsResource obtener(String imagenId) {
        if (imagenId == null) {
            throw new RecursoNoEncontradoException("No tiene imagen propia.");
        }
        GridFSFile archivo = gridFs.findOne(porId(imagenId));
        if (archivo == null) {
            throw new RecursoNoEncontradoException("No se encontró la imagen.");
        }
        return gridFs.getResource(archivo);
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
    public static int[] leerDimensiones(byte[] b, String tipo) {
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
    public static String detectarTipoImagen(byte[] b) {
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
