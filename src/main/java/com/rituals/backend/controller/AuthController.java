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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final AppUserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            AppUser user = authService.registerUser(request);
            // Auto login after registration
            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getUsername(), user.getTotalPoints()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Registration failed."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect email or password"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Login failed. Please check your credentials."));
        }

        try {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails);
            
            AppUser user = userRepository.findByEmail(authRequest.getEmail()).orElseThrow();

            return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getUsername(), user.getTotalPoints()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Could not complete login. Please try again."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }

        return userRepository.findByEmail(email).map(user -> {
            String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(email, token);

            return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset token has been sent."));
        }).orElse(ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset token has been sent.")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (email == null || token == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email, token, and new password are required."));
        }

        if (newPassword.length() < 8 || !newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*[a-z].*") || !newPassword.matches(".*\\d.*") || !newPassword.matches(".*[!@#$%^&*()-+=_].*")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character."));
        }

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getResetToken() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid reset request."));
        }

        if (!user.getResetToken().equals(token)) {
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
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        String email = auth.getName();
        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        
        Integer age = null;
        if (user.getDateOfBirth() != null) {
            age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        }

        java.util.Map<String, Object> profile = new java.util.HashMap<>();
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
