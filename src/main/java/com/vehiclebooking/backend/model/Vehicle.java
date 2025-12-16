package com.vehiclebooking.backend.model;

import com.vehiclebooking.backend.model.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User assignedDriver;

    @Enumerated(EnumType.STRING)
    private VehicleType type;

    private String model;
    private String numberPlate;

    @ElementCollection
    private List<String> images;

    private int capacity;

    private BigDecimal pricePerKm;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;

    private boolean isAvailable;

    private double latitude;
    private double longitude;
}
