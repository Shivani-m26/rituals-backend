package com.rituals.backend.service;

import com.rituals.backend.dto.RegisterRequest;
import com.rituals.backend.entity.AppUser;
import com.rituals.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Password must be >= 8 chars, with at least 1 uppercase, 1 lowercase, 1
    // number, and 1 special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]).{8,}$");

    @Transactional
    public AppUser registerUser(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be empty.");
        }

        // Validate & normalize email
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()
                || !com.rituals.backend.util.EmailDomainValidator.isRealEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Please enter a valid, active email address.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Email is already taken!");
        }

        // Validate & normalize username
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        String normalizedUsername = request.getUsername().trim();
        if (normalizedUsername.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("Username is already taken!");
        }

        // Validate password
        String pwd = request.getPassword();
        if (pwd == null || !PASSWORD_PATTERN.matcher(pwd).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character.");
        }

        // Validate date of birth
        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Date of birth cannot be in the future.");
            }
            if (request.getDateOfBirth().isBefore(LocalDate.of(1900, 1, 1))) {
                throw new IllegalArgumentException("Please enter a valid date of birth.");
            }
        }

        AppUser user = AppUser.builder()
                .email(normalizedEmail)
                .username(normalizedUsername)
                .password(passwordEncoder.encode(pwd))
                .gender(request.getGender() != null ? request.getGender().trim() : null)
                .dateOfBirth(request.getDateOfBirth())
                .totalPoints(0)
                .lastLoginAt(java.time.LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }
}
