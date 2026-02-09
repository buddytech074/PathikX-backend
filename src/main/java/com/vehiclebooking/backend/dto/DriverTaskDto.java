package com.vehiclebooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverTaskDto {
    private String type; // "PICKUP" or "DROP"
    private String location;
    private double latitude;
    private double longitude;
    private Long bookingId;
    private String customerName;
    private String customerPhone;
    private String otp; // For Pickups
    private int sequence;
    private String status; // Status of this specific task
}
