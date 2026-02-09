package com.vehiclebooking.backend.dto;

import lombok.Data;

@Data
public class SaveOtpRequest {
    private String phoneNumber;
    private String otp;
}
