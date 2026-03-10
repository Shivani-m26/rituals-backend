package com.rituals.backend.controller;

import com.rituals.backend.dto.ContactRequest;
import com.rituals.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;

    @PostMapping("/contact")
    public ResponseEntity<?> submitContactForm(@RequestBody ContactRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        if (request.getComments() == null || request.getComments().isBlank()) {
            return ResponseEntity.badRequest().body("Comments are required.");
        }

        emailService.sendContactFormEmail(
            request.getName() != null ? request.getName() : "Anonymous",
            request.getEmail(),
            request.getPhone() != null ? request.getPhone() : "Not provided",
            request.getComments()
        );

        return ResponseEntity.ok(Map.of("message", "Thank you for reaching out! We'll get back to you soon. ✨"));
    }
}
