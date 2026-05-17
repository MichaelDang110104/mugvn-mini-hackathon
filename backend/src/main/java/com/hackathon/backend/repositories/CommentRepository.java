package com.hackathon.backend.repositories;

import com.hackathon.backend.models.Comment;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, ObjectId> {

    List<Comment> findByMovieId(ObjectId movieId);

    Page<Comment> findByMovieId(ObjectId movieId, Pageable pageable);

    List<Comment> findByEmail(String email);

    List<Comment> findByName(String name);
}
