package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.SearchResponse;
import com.hackathon.backend.services.MovieSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final MovieSearchService movieSearchService;

    @GetMapping("/movies")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId) {

        SearchResponse response = movieSearchService.search(q, limit);
        return ResponseEntity.ok(response);
    }
}
