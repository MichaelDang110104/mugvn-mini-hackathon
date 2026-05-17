package com.hackathon.backend.controllers;

import com.hackathon.backend.models.Theater;
import com.hackathon.backend.repositories.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theaters")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterRepository theaterRepository;

    @GetMapping
    public Page<Theater> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return theaterRepository.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Theater> getById(@PathVariable String id) {
        return theaterRepository.findById(new ObjectId(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/theaterId/{theaterId}")
    public ResponseEntity<Theater> getByTheaterId(@PathVariable Integer theaterId) {
        return theaterRepository.findByTheaterId(theaterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/state/{state}")
    public List<Theater> getByState(@PathVariable String state) {
        return theaterRepository.findByLocationAddressState(state);
    }

    @GetMapping("/city/{city}")
    public List<Theater> getByCity(@PathVariable String city) {
        return theaterRepository.findByLocationAddressCity(city);
    }

    @GetMapping("/zipcode/{zipcode}")
    public List<Theater> getByZipcode(@PathVariable String zipcode) {
        return theaterRepository.findByLocationAddressZipcode(zipcode);
    }
}
