package com.rituals.backend.controller;

import com.rituals.backend.dto.AuthRequest;
import com.rituals.backend.dto.AuthResponse;
import com.rituals.backend.dto.RegisterRequest;
import com.rituals.backend.entity.AppUser;
import com.rituals.backend.repository.AppUserRepository;
import com.rituals.backend.security.CustomUserDetailsService;
import com.rituals.backend.security.JwtUtil;
import com.rituals.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final AppUserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            AppUser user = authService.registerUser(request);
            // Auto login after registration
            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            final String jwt = jwtUtil.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getUsername(), user.getTotalPoints()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body("Incorrect email or password");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());
        final String jwt = jwtUtil.generateToken(userDetails);
        
        AppUser user = userRepository.findByEmail(authRequest.getEmail()).orElseThrow();

        return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getUsername(), user.getTotalPoints()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByEmail(email).orElseThrow();
        
        return ResponseEntity.ok(java.util.Map.of(
            "username", user.getUsername(),
            "email", user.getEmail(),
            "totalPoints", user.getTotalPoints(),
            "badges", user.getBadges(),
            "habitCount", user.getHabits() != null ? user.getHabits().size() : 0
        ));
    }
}
