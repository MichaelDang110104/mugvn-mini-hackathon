package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.HomeFeedResponse;
import com.hackathon.backend.services.HomeFeedService;
import com.hackathon.backend.services.UserIdResolver;
import lombok.RequiredArgsConstructor;
import com.hackathon.backend.models.Movie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeController {

    private final HomeFeedService homeFeedService;
    private final UserIdResolver userIdResolver;

    @GetMapping("/home")
    public CompletableFuture<ResponseEntity<HomeFeedResponse>> home() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails)
                ? userDetails.getUsername()
                : null;
        String userId = userIdResolver.resolve(principal);
        return homeFeedService.buildFeed(userId).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/home/sections/{sectionId}")
    public CompletableFuture<ResponseEntity<List<Movie>>> loadSection(
            @PathVariable String sectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails)
                ? userDetails.getUsername()
                : null;
        String userId = userIdResolver.resolve(principal);
        return homeFeedService.loadSection(userId, sectionId, page, limit)
                .thenApply(ResponseEntity::ok);
    }
}
