package gr.aegean.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        @NotBlank
        private String fullName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String token;
        private String username;
        private String fullName;
    }
}
