package com.example.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class AuthDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Login request")
    public static class LoginRequest {
        @NotBlank
        @Schema(example = "admin")
        private String username;

        @NotBlank
        @Schema(example = "admin123")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Register request")
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        @Schema(example = "john_doe")
        private String username;

        @NotBlank
        @Size(min = 6, max = 100)
        @Schema(example = "password123")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "JWT token response")
    public static class TokenResponse {
        @Schema(description = "JWT access token")
        private String token;

        @Schema(description = "Token type", example = "Bearer")
        private String type;

        @Schema(description = "Username")
        private String username;

        @Schema(description = "User role")
        private String role;

        @Schema(description = "Expires in (ms)")
        private Long expiresIn;
    }
}
