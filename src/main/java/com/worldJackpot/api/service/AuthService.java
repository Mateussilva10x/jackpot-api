package com.worldJackpot.api.service;

import com.worldJackpot.api.dto.auth.AuthDto;
import com.worldJackpot.api.model.User;
import com.worldJackpot.api.model.enums.UserRole;
import com.worldJackpot.api.model.PasswordResetToken;
import com.worldJackpot.api.repository.PasswordResetTokenRepository;
import com.worldJackpot.api.repository.UserRepository;
import com.worldJackpot.api.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordResetTokenRepository tokenRepository;
    private final SupabaseAuthService supabaseAuthService;
    private final String frontendUrl;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            PasswordResetTokenRepository tokenRepository,
            SupabaseAuthService supabaseAuthService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.tokenRepository = tokenRepository;
        this.supabaseAuthService = supabaseAuthService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
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
                .avatarId(request.getAvatarId())
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
        
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthDto.AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarId(user.getAvatarId())
                .build();
    }

    public AuthDto.AuthResponse getMe(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String jwt = tokenProvider.generateToken(authentication);

        return AuthDto.AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarId(user.getAvatarId())
                .build();
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Invalidate any existing token for this user
        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        supabaseAuthService.sendResetPasswordEmail(user.getEmail(), resetLink, user.getName());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete token after successful reset
        tokenRepository.delete(resetToken);
    }
}
