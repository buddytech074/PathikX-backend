package com.vehiclebooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerVerificationDto {
    private Long id;
    private Long bookingId;
    private Long passengerId;
    private String passengerName;
    private String passengerPhone;
    private String pickupOtp;
    private String dropOtp;
    private Boolean pickupVerified;
    private Boolean dropVerified;
    private String pickupLocation;
    private String dropLocation;
}
