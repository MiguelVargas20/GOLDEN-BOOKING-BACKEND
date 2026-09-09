package com.sena.goldenbooking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.sena.goldenbooking.models.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    @Query("{ 'docId.numeroD' : ?0 }")
    Optional<Usuario> findByDocNum(String docnum);

    @Query(value = "{ 'docId.numeroD' : ?0 }", exists = true)
    boolean existsByDocNum(String docnum);

    Optional<Usuario> findByCorreo(String correo);

    // FIX hallazgo #9: faltaba este método. Usuario.correo tiene @Indexed(unique = true)
    // en Mongo, pero nada en el service lo comprobaba antes de guardar — un segundo
    // registro con el mismo correo (documento/username distintos) reventaba con
    // DuplicateKeyException, sin mapear en GlobalExceptionHandler, y el usuario veía
    // un 500 genérico en vez de un mensaje claro.
    boolean existsByCorreo(String correo);

    // FIX hallazgo #11 (N+1 en RecordatorioService): antes se buscaba el usuario
    // UNO POR UNO dentro de un forEach (una consulta a Mongo por cada reserva de la
    // ventana de recordatorio). Con esta consulta $in se trae a todos los usuarios
    // involucrados en una sola ida a la base de datos.
    @Query("{ 'docId.numeroD' : { $in: ?0 } }")
    List<Usuario> findByDocNumIn(List<String> docnums);
}