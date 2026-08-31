package gr.aegean.cinema.controller;

import gr.aegean.cinema.dto.AuthDTO;
import gr.aegean.cinema.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthDTO.TokenResponse> register(
            @Valid @RequestBody AuthDTO.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    /**
     * POST /api/auth/login
     * Authenticate and receive a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthDTO.TokenResponse> login(
            @Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
