package com.vehiclebooking.backend.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String phoneNumber;
    private String otp;
    private String firebaseToken; // ID Token from Frontend
    private String userDetails; // Optional name/email if registering
}
