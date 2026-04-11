package com.sena.goldenbooking.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.Club;

public interface ClubRepository extends MongoRepository<Club, String> {

}
