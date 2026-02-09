package com.vehiclebooking.backend.model;

import com.vehiclebooking.backend.model.enums.BookingStatus;
import com.vehiclebooking.backend.model.enums.RefundStatus;
import com.vehiclebooking.backend.model.enums.TripType;
import com.vehiclebooking.backend.model.enums.VehicleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "parentBooking", "subBookings" })
@EqualsAndHashCode(exclude = { "parentBooking", "subBookings" })
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = true)
    @JoinColumn(name = "vehicle_id", nullable = true)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type")
    private TripType tripType;

    private String pickupLocation;
    private String dropLocation;

    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;

    // Passenger count (1, 3, or 5+)
    private int passengerCount;

    // Distance and pricing
    private double estimatedDistance; // in kilometers
    private BigDecimal minEstimatedPrice;
    private BigDecimal maxEstimatedPrice;
    private BigDecimal actualPrice; // Set when driver accepts

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime; // Optional if immediate ride without end time specific

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BookingStatus status;

    private BigDecimal platformCharge;
    private BigDecimal remainingAmount;
    private BigDecimal totalAmount;

    // OTP for ride verification (unique per booking)
    @Column(length = 4)
    private String rideOtp;

    // Payment tracking
    private Boolean paymentRequired; // true if payment needed before confirmation
    private Boolean paymentCompleted; // true if payment successfully received

    // Cancellation & Refund
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;

    private LocalDateTime createdAt;

    // Hierarchical bookings (for Wedding Fleet)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_booking_id")
    @JsonIgnore
    private Booking parentBooking;

    @OneToMany(mappedBy = "parentBooking", cascade = CascadeType.ALL)
    private java.util.List<Booking> subBookings;

    private Integer quantity; // Total vehicles in parent booking
    private Boolean isWedding;
    private Boolean isShared; // true for Partnership/Share, false for Reserve

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private java.util.List<BookingStop> stops;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PassengerVerification> passengerVerifications;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Generate 4-digit OTP
        rideOtp = String.format("%04d", (int) (Math.random() * 10000));

        // Initialize flags
        if (paymentRequired == null)
            paymentRequired = false;
        if (paymentCompleted == null)
            paymentCompleted = false;
        if (refundStatus == null)
            refundStatus = RefundStatus.NONE;
        if (isWedding == null)
            isWedding = false;
        if (isShared == null)
            isShared = false;
    }

    /**
     * Helper method to determine if this booking is a prebook (future booking)
     * Prebook is defined as booking with startDateTime more than 1 hour in the
     * future
     */
    public boolean isPrebook() {
        if (startDateTime == null) {
            return false;
        }
        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        return startDateTime.isAfter(oneHourFromNow);
    }

    public Long getParentBookingId() {
        return parentBooking != null ? parentBooking.getId() : null;
    }
}
