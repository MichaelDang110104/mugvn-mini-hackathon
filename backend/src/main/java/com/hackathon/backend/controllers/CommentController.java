package com.hackathon.backend.controllers;

import com.hackathon.backend.models.Comment;
import com.hackathon.backend.repositories.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentRepository commentRepository;

    @GetMapping
    public Page<Comment> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commentRepository.findAll(
                PageRequest.of(page, size, Sort.by("date").descending()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> getById(@PathVariable String id) {
        return commentRepository.findById(new ObjectId(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/movie/{movieId}")
    public Page<Comment> getByMovieId(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commentRepository.findByMovieId(
                new ObjectId(movieId),
                PageRequest.of(page, size, Sort.by("date").descending()));
    }

    @GetMapping("/email/{email}")
    public List<Comment> getByEmail(@PathVariable String email) {
        return commentRepository.findByEmail(email);
    }

    @GetMapping("/user/{name}")
    public List<Comment> getByUser(@PathVariable String name) {
        return commentRepository.findByName(name);
    }
}
