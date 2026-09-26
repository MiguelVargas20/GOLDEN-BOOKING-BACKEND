package com.sena.goldenbooking.membresias.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.membresias.model.ConfigMembresia;

public interface ConfigMembresiaRepository extends MongoRepository<ConfigMembresia, String> {
}
