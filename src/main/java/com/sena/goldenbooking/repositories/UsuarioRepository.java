package com.sena.goldenbooking.repositories;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.sena.goldenbooking.models.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    @Query("{ 'docId.numeroD' : ?0 }")
    Optional<Usuario> findByDocNum(String docnum);

    @Query(value = "{ 'docId.numeroD' : ?0 }", exists = true)
    boolean existsByDocNum(String docnum);
}