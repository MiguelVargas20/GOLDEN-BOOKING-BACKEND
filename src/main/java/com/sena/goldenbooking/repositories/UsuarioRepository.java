package com.sena.goldenbooking.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.sena.goldenbooking.models.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String>{
    //Consulta user por Num Doc manual indica a Mongo a buscar en el objeto Doc campo Num
    @Query("{'doc.numero': ?0")
    Optional<Usuario> findByDocNum(String docnum);

    //Spring genera la consulta automaticamente 
    Optional<Usuario> findByDocNumero(String numero);

    //Encuentra y elimina segun Docnum un User
    @Query(value = "{'doc.numero': ?0}", delete = true)
    void deleteByDocNum(String docnum);
    
    //Encuentra y valida si existe por DocNum
    @Query(value = "{'doc.numero': ?0}", exists = true)
    Boolean existsByDocNum(String docnum);

}
