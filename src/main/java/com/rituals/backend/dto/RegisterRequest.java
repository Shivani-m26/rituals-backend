package com.rituals.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {
    private String email;
    private String username;
    private String password;
    private String gender;
    private LocalDate dateOfBirth;
}
