package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.AuthRequest;
import com.hackathon.backend.dto.AuthResponse;
import com.hackathon.backend.models.MflixUser;
import com.hackathon.backend.repositories.MflixUserRepository;
import com.hackathon.backend.repositories.UserOnboardingAnswersRepository;
import com.hackathon.backend.security.JwtUtil;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private MflixUserRepository userRepository;

    @Mock
    private UserOnboardingAnswersRepository userOnboardingAnswersRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthController controller;

    @Test
    void login_returnsOnboardingCompletionFlag() {
        ObjectId userId = new ObjectId("507f1f77bcf86cd799439011");
        MflixUser user = new MflixUser();
        user.setId(userId);
        user.setEmail("user@example.com");
        UserDetails userDetails = User.withUsername("user@example.com").password("pw").authorities("ROLE_USER").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails, userId.toHexString())).thenReturn("jwt-token");
        when(userOnboardingAnswersRepository.findTopByUserIdOrderByCompletedAtDesc(userId.toHexString()))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.login(AuthRequest.builder().email("user@example.com").build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        AuthResponse body = (AuthResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isOnboardingComplete()).isFalse();
    }
}
