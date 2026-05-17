package com.hackathon.backend.services;

import com.hackathon.backend.models.AppUser;
import com.hackathon.backend.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Manages anonymous session-based identity.
 * Mints new sessions when needed, returns existing sessions when found.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final AppUserRepository appUserRepository;

    /**
     * Resolve a sessionId from the request. If neither header nor param is provided,
     * a new anonymous session is minted.
     *
     * @param headerSessionId from X-Session-Id header
     * @param paramSessionId  from sessionId query parameter
     * @return resolved sessionId
     */
    public String resolveSessionId(String headerSessionId, String paramSessionId) {
        // Contract: if both are supplied and disagree, reject
        if (headerSessionId != null && paramSessionId != null
                && !headerSessionId.equals(paramSessionId)) {
            throw new SessionConflictException(
                    "X-Session-Id header and sessionId parameter disagree");
        }

        String sessionId = headerSessionId != null ? headerSessionId : paramSessionId;

        if (sessionId == null || sessionId.isBlank()) {
            // Mint a new anonymous session
            sessionId = UUID.randomUUID().toString();
            createAnonymousUser(sessionId);
            log.info("Minted new anonymous session: {}", sessionId);
        } else {
            // Touch the existing session
            touchSession(sessionId);
        }

        return sessionId;
    }

    private void createAnonymousUser(String sessionId) {
        AppUser user = AppUser.builder()
                .sessionId(sessionId)
                .createdAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build();
        try {
            appUserRepository.save(user);
        } catch (Exception e) {
            log.warn("Could not persist anonymous user for session [{}]: {}", sessionId, e.getMessage());
        }
    }

    private void touchSession(String sessionId) {
        appUserRepository.findBySessionId(sessionId).ifPresent(user -> {
            user.setLastSeenAt(Instant.now());
            appUserRepository.save(user);
        });
    }

    /**
     * Thrown when X-Session-Id header and sessionId parameter disagree.
     */
    public static class SessionConflictException extends RuntimeException {
        public SessionConflictException(String message) {
            super(message);
        }
    }
}
