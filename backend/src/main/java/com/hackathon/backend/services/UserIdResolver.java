package com.hackathon.backend.services;

import com.hackathon.backend.repositories.MflixUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves an incoming principal (typically the authenticated email/username) to the
 * canonical user identifier used across stored data — the MflixUser ObjectId hex.
 *
 * <p>{@code user_profiles.userId} and {@code user_events.userId} are both keyed by the
 * MflixUser ObjectId hex, while Spring Security exposes the email as the principal name.
 * Callers must resolve before querying those collections, otherwise lookups silently miss.
 *
 * <p>If the value is not a known email it is passed through unchanged, so an ObjectId hex
 * or anonymous session id remains usable.
 */
@Service
@RequiredArgsConstructor
public class UserIdResolver {

    private final MflixUserRepository mflixUserRepository;

    public String resolve(String principal) {
        if (principal == null || principal.isBlank()) {
            return null;
        }
        return mflixUserRepository.findByEmail(principal)
                .map(user -> user.getId().toHexString())
                .orElse(principal);
    }
}
