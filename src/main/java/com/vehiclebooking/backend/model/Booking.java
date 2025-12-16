package com.vehiclebooking.backend.model;

import com.vehiclebooking.backend.model.enums.BookingStatus;
import com.vehiclebooking.backend.model.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = true)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    private String pickupLocation;
    private String dropLocation;

    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime; // Optional if immediate ride without end time specific

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private BigDecimal platformCharge;
    private BigDecimal remainingAmount;
    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
