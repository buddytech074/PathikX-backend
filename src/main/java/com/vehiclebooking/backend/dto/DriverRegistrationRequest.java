package com.vehiclebooking.backend.dto;

import lombok.Data;

@Data
public class DriverRegistrationRequest {
    private String idToken; // Google ID token
    private Long userId; // For existing users upgrading to driver
    private String licenseNumber;
    private String licenseFrontUrl; // Base64 encoded image
    private String licenseBackUrl; // Base64 encoded image
}
