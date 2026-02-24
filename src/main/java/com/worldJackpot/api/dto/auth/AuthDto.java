package com.worldJackpot.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegisterRequest {
        @Schema(description = "User's full name", example = "John Doe")
        @NotBlank(message = "Name is required")
        private String name;

        @Schema(description = "User's email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @Schema(description = "User's password", example = "StrongP@ssw0rd")
        @NotBlank(message = "Password is required")
        private String password;

        @Schema(description = "Optional user role (e.g. USER, ADMIN)", example = "USER", defaultValue = "USER")
        private String role;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRequest {
        @Schema(description = "User's email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @Schema(description = "User's password", example = "StrongP@ssw0rd")
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthResponse {
        @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String token;
        
        @Schema(description = "User ID", example = "1")
        private Long id;
        
        @Schema(description = "User's role", example = "USER")
        private String role;
        
        @Schema(description = "User's email", example = "john.doe@example.com")
        private String email;
    }
}
