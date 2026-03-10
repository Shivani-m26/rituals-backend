package com.rituals.backend.dto;

import lombok.Data;

@Data
public class ContactRequest {
    private String name;
    private String email;
    private String phone;
    private String comments;
}
