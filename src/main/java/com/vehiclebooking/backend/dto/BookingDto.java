package com.vehiclebooking.backend.dto;

import com.vehiclebooking.backend.model.enums.TripType;
import com.vehiclebooking.backend.model.enums.VehicleType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long vehicleId;
    private VehicleType vehicleType;
    private TripType tripType;
    private String pickupLocation;
    private String dropLocation;
    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;
    private Integer passengerCount; // 1, 3, or 5+
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    // Wedding Fleet
    private Boolean isWedding;
    private Boolean isShared; // true for Partnership (sharing), false for Reserve (private)
    private java.util.Map<VehicleType, Integer> fleet; // vehicleType -> quantity

    // Multiple stops for Toto EV/Shared rides
    private java.util.List<BookingStopDto> stops;
}
