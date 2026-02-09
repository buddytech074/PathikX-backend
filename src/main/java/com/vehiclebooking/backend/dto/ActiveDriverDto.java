package com.vehiclebooking.backend.dto;

import com.vehiclebooking.backend.model.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveDriverDto {
    private Long vehicleId;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private VehicleType vehicleType;
    private String vehicleModel;
    private String numberPlate;
    private double latitude;
    private double longitude;
    private boolean isAvailable;
}
