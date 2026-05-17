package com.hackathon.backend.repositories;

import com.hackathon.backend.models.Theater;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TheaterRepository extends MongoRepository<Theater, ObjectId> {

    Optional<Theater> findByTheaterId(Integer theaterId);

    List<Theater> findByLocationAddressState(String state);

    List<Theater> findByLocationAddressCity(String city);

    List<Theater> findByLocationAddressZipcode(String zipcode);
}
