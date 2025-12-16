package com.vehiclebooking.backend.dto;

import com.vehiclebooking.backend.model.enums.VehicleType;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class VehicleDto {
    private VehicleType type;
    private String model;
    private String numberPlate;
    private List<String> images;
    private int capacity;
    private BigDecimal pricePerKm;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    private boolean isAvailable;
    private double latitude;
    private double longitude;
}
