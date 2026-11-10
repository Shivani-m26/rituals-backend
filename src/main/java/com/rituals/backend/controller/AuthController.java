package com.rituals.backend.controller;

import com.rituals.backend.dto.AuthRequest;
import com.rituals.backend.dto.AuthResponse;
import com.rituals.backend.dto.RegisterRequest;
import com.rituals.backend.entity.AppUser;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.security.CustomUserDetailsService;
import com.rituals.backend.security.JwtUtil;
import com.rituals.backend.service.AuthService;
import com.rituals.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final AppUserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]).{8,}$"
    );

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        log.info("[AUTH] Registration attempt for email: '{}'", request != null ? request.getEmail() : "null");
        try {
            AppUser user = authService.registerUser(request);
            log.info("[AUTH] Registration successful! Created user: '{}' (id: {}). Dispatching welcome email...", user.getUsername(), user.getId());
            // Send asynchronous welcome email to the user
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());

            // Auto login after registration using normalized email
            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getUsername(), user.getTotalPoints()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Registration failed."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) {
        if (authRequest == null || authRequest.getEmail() == null || authRequest.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        }

        String email = authRequest.getEmail().trim().toLowerCase();
        String password = authRequest.getPassword();

        if (email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password cannot be empty."));
        }

        log.info("[AUTH] Login attempt for email: '{}'", email);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            log.warn("[AUTH] Login failed for '{}': {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Incorrect email or password"));
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("[AUTH] Authentication exception for '{}': {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Incorrect email or password"));
        } catch (Exception e) {
            log.error("[AUTH] Unexpected error during login for '{}': {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Login failed. Please check your credentials."));
        }

        try {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            final String jwt = jwtUtil.generateToken(userDetails);
            AppUser user = userRepository.findByEmail(email).orElseThrow();
            log.info("[AUTH] Login successful for: '{}' (id: {})", user.getEmail(), user.getId());

            // Check if user is returning after >= 7 days
            LocalDateTime now = LocalDateTime.now();
            if (user.getLastLoginAt() != null && user.getLastLoginAt().isBefore(now.minusDays(7))) {
                emailService.sendLoginAlertEmail(user.getEmail(), user.getUsername(), user.getLastLoginAt());
            }
            user.setLastLoginAt(now);
            userRepository.save(user);

            return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getUsername(), user.getTotalPoints()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Could not complete login. Please try again."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            log.warn("[AUTH] /forgot-password rejected: email is blank");
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }

        String normalizedEmail = email.trim().toLowerCase();
        log.info("[AUTH] Received forgot-password request for: '{}'", normalizedEmail);

        AppUser user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            log.warn("[AUTH] No account found in database matching email: '{}'. No reset email will be sent.", normalizedEmail);
            return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset token has been sent."));
        }

        String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        log.info("[AUTH] Generated reset token for '{}'. Dispatching email now...", normalizedEmail);
        emailService.sendPasswordResetEmail(normalizedEmail, token);

        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset token has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (email == null || token == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email, token, and new password are required."));
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedToken = token.trim();

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character."));
        }

        AppUser user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.getResetToken() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid reset request."));
        }

        if (!user.getResetToken().equalsIgnoreCase(normalizedToken)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired reset token."));
        }

        if (user.getResetTokenExpiry() != null && user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Reset token has expired. Please request a new one."));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully. You can now login."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        String email = auth.getName();
        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        
        Integer age = null;
        if (user.getDateOfBirth() != null) {
            age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        }

        Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("gender", user.getGender());
        profile.put("dateOfBirth", user.getDateOfBirth());
        profile.put("age", age);
        profile.put("totalPoints", user.getTotalPoints());
        profile.put("badges", user.getBadges());
        profile.put("habitCount", user.getHabits() != null ? user.getHabits().size() : 0);

        return ResponseEntity.ok(profile);
    }
}
