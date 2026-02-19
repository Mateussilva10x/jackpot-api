package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.auth.AuthDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.model.enums.UserRole;
import com.worldJackpot.api.repository.UserRepository;
import com.worldJackpot.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already pending or registered");
        }

        UserRole role = UserRole.USER;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .totalPoints(0)
                .rankingPosition(0)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-login after registration
        return login(new AuthDto.LoginRequest(request.getEmail(), request.getPassword()));
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthDto.AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
