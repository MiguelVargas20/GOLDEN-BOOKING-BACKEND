package com.sena.goldenbooking.usuarios.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.usuarios.model.UsuarioAuth;

public interface UsuarioAuthRepository extends MongoRepository<UsuarioAuth, String> {
    
  // Cambiado de findByUsr a findByUser para que coincida con el campo 'private String user'
    Optional<UsuarioAuth> findByUser(String user);
    
    boolean existsByUser(String user);

}
