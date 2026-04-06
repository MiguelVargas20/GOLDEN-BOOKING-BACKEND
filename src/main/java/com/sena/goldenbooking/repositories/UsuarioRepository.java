package com.sena.goldenbooking.repositories;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.sena.goldenbooking.models.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    @Query("{'docId.numeroDocumento': ?0}") // numeroDocumento es el campo dentro de DocumentoDto/Model
    Optional<Usuario> findByDocNum(String docnum);

    // Este es automático de Spring. Funciona si en tu clase Usuario el campo es "doc" 
    // y en la clase Documento el campo es "numero".
    Optional<Usuario> findByDocId_NumeroDocumento(String numeroDocumento);

    // Para eliminar por número de documento
    @Query(value = "{'doc.numero': ?0}", delete = true)
    void deleteByDocNum(String docnum);
    
    // Para validar existencia por número de documento
    @Query(value = "{'doc.numero': ?0}", exists = true)
    boolean existsByDocNum(String docnum);
}