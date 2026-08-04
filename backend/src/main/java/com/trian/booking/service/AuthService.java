package com.trian.booking.service;

import com.trian.booking.dto.AuthRequestDTO;
import com.trian.booking.dto.AuthResponseDTO;
import com.trian.booking.model.Role;
import com.trian.booking.model.User;
import com.trian.booking.repository.UserRepository;
import com.trian.booking.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponseDTO register(AuthRequestDTO request) {
        String email = normalizeEmail(request.getEmail());
        if (email.isEmpty() || request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Email and a password of at least 6 characters are required");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }

        User user = new User(email, passwordEncoder.encode(request.getPassword()), Role.USER);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponseDTO(token, user.getEmail(), user.getRole().name());
    }

    public AuthResponseDTO login(AuthRequestDTO request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Invalid email or password"));

        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalStateException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponseDTO(token, user.getEmail(), user.getRole().name());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
