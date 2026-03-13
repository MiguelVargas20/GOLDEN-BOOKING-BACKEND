package com.sena.goldenbooking.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.Usuario;

public interface UsuarioAuthRepository extends MongoRepository<Usuario, String>{

}
