package com.hackathon.backend.security;

import com.hackathon.backend.models.MflixUser;
import com.hackathon.backend.repositories.MflixUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MflixUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        MflixUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
                
        // In the sample dataset, passwords might be hashed with bcrypt.
        // We set empty authorities for this hackathon MVP.
        return new User(user.getEmail(), user.getPassword(), new ArrayList<>());
    }
}
