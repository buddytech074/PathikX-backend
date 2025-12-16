package com.vehiclebooking.backend.dto;

import com.vehiclebooking.backend.model.enums.VehicleType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long vehicleId;
    private VehicleType vehicleType;
    private String pickupLocation;
    private String dropLocation;
    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}
