package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.AuthRequest;
import com.hackathon.backend.dto.AuthResponse;
import com.hackathon.backend.models.MflixUser;
import com.hackathon.backend.repositories.MflixUserRepository;
import com.hackathon.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // Note: We bypass AuthenticationManager for hackathon mock data if the passwords in the DB 
    // aren't matched properly with BCrypt. We directly check user existence.
    private final MflixUserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        
        MflixUser user = userRepository.findByEmail(authRequest.getEmail())
                .orElse(null);
                
        if (user == null) {
            return ResponseEntity.status(401).body("Invalid email");
        }
        
        // For Hackathon MVP: We assume password matches since we are mocking day-to-day real user tracking.
        // In a real app, you would use: authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getId().toHexString());
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .userId(user.getId().toHexString())
                .email(user.getEmail())
                .build());
    }
}
