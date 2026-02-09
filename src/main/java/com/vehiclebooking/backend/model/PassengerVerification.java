package com.vehiclebooking.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "passenger_verifications")
public class PassengerVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "passengerVerifications", "vehicle", "user" })
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "bookings", "vehicles", "transactions" })
    private User passenger;

    @Column(name = "pickup_otp", length = 4)
    private String pickupOtp;

    @Column(name = "drop_otp", length = 4)
    private String dropOtp;

    @Column(name = "pickup_verified")
    private Boolean pickupVerified;

    @Column(name = "drop_verified")
    private Boolean dropVerified;

    @Column(name = "pickup_verified_at")
    private LocalDateTime pickupVerifiedAt;

    @Column(name = "drop_verified_at")
    private LocalDateTime dropVerifiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (pickupVerified == null)
            pickupVerified = false;
        if (dropVerified == null)
            dropVerified = false;

        // Generate OTPs
        if (pickupOtp == null) {
            pickupOtp = String.format("%04d", (int) (Math.random() * 10000));
        }
        if (dropOtp == null) {
            dropOtp = String.format("%04d", (int) (Math.random() * 10000));
        }
    }
}
