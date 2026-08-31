package gr.aegean.cinema.service;

import gr.aegean.cinema.dto.AuthDTO;
import gr.aegean.cinema.exception.ConflictException;
import gr.aegean.cinema.model.entity.User;
import gr.aegean.cinema.repository.UserRepository;
import gr.aegean.cinema.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthDTO.TokenResponse register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already taken: " + request.getUsername());
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .build();
        userRepository.save(user);
        log.info("User registered: {}", user.getUsername());

        String token = jwtUtils.generateToken(user.getUsername());
        return new AuthDTO.TokenResponse(token, user.getUsername(), user.getFullName());
    }

    public AuthDTO.TokenResponse login(AuthDTO.LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String token = jwtUtils.generateToken(auth.getName());
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        log.info("User logged in: {}", user.getUsername());
        return new AuthDTO.TokenResponse(token, user.getUsername(), user.getFullName());
    }
}
