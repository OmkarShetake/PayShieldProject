package com.payshield.auth.dto;

import com.payshield.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

public class AuthDTOs {

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
        @NotBlank
        private String fullName;
        private User.Role role = User.Role.MERCHANT;
    }

    @Data
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private UserResponse user;

        public TokenResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.user = user;
        }
    }

    @Data
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class UserResponse {
        private UUID id;
        private String email;
        private String fullName;
        private User.Role role;

        public static UserResponse from(User user) {
            UserResponse r = new UserResponse();
            r.id = user.getId();
            r.email = user.getEmail();
            r.fullName = user.getFullName();
            r.role = user.getRole();
            return r;
        }
    }
}
