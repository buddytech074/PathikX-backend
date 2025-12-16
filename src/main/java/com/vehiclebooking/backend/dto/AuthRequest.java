package com.vehiclebooking.backend.dto;

import com.vehiclebooking.backend.model.enums.Role;
import lombok.Data;

@Data
public class AuthRequest {
    private String phoneNumber;
    private String password;
    private String fullName;
    private Role role; // Optional during login, required during registration if implicit
}
