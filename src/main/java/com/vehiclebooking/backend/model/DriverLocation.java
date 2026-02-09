package com.vehiclebooking.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "driver_locations")
public class DriverLocation {

    @Id
    private Long driverId; // Same as User ID

    private double latitude;
    private double longitude;

    private LocalDateTime updatedAt;

    private Double heading; // Direction in degrees (0-360)

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
