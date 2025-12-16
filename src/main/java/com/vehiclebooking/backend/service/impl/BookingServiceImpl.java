package com.vehiclebooking.backend.service.impl;

import com.vehiclebooking.backend.dto.BookingDto;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.model.enums.BookingStatus;
import com.vehiclebooking.backend.repository.BookingRepository;
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.repository.VehicleRepository;
import com.vehiclebooking.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public Booking createBooking(Long userId, BookingDto bookingDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Vehicle vehicle = null;

        // If specific vehicle is requested
        if (bookingDto.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(bookingDto.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

            // Overlap Check for Future Bookings
            if (bookingDto.getStartDateTime() != null && bookingDto.getEndDateTime() != null) {
                List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                        vehicle.getId(),
                        List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED),
                        bookingDto.getStartDateTime(),
                        bookingDto.getEndDateTime());
                if (!conflicts.isEmpty()) {
                    throw new RuntimeException("Vehicle is already booked for this time slot");
                }
            }

            if (!vehicle.isAvailable()) {
                throw new RuntimeException("Vehicle is currently offline (Driver inactive)");
            }
        } else if (bookingDto.getVehicleType() == null) {
            throw new RuntimeException("Either Vehicle ID or Vehicle Type must be provided");
        }

        Booking booking = Booking.builder()
                .user(user)
                .vehicle(vehicle) // Can be null now
                .vehicleType(bookingDto.getVehicleType() != null ? bookingDto.getVehicleType()
                        : (vehicle != null ? vehicle.getType() : null))
                .pickupLocation(bookingDto.getPickupLocation())
                .dropLocation(bookingDto.getDropLocation())
                .pickupLat(bookingDto.getPickupLat())
                .pickupLng(bookingDto.getPickupLng())
                .dropLat(bookingDto.getDropLat())
                .dropLng(bookingDto.getDropLng())
                .startDateTime(bookingDto.getStartDateTime())
                .endDateTime(bookingDto.getEndDateTime())
                .status(BookingStatus.PENDING)
                .platformCharge(new BigDecimal("50.00")) // Base charge, specific pricing calc moves to Accept/Complete
                .build();

        return bookingRepository.save(booking);
    }

    // ... existing methods ...

    @Override
    public List<Booking> getDriverBookings(Long driverId) {
        return bookingRepository.findByDriverId(driverId);
    }

    @Override
    public Booking acceptBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.setStatus(BookingStatus.ACCEPTED);
        return bookingRepository.save(booking);
    }

    @Override
    public Booking completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.setStatus(BookingStatus.COMPLETED);

        // Mocking distance/time calculation for MVP (e.g., 100km ride)
        BigDecimal distanceKm = new BigDecimal("50.0");
        BigDecimal pricePerKm = booking.getVehicle().getPricePerKm() != null ? booking.getVehicle().getPricePerKm()
                : BigDecimal.ZERO;

        BigDecimal vehicleCharge = pricePerKm.multiply(distanceKm);

        // If daily/hourly, add logic here. Using simple Km logic for now.
        if (vehicleCharge.compareTo(BigDecimal.ZERO) == 0 && booking.getVehicle().getPricePerDay() != null) {
            vehicleCharge = booking.getVehicle().getPricePerDay(); // fallback to 1 day charge
        }

        booking.setRemainingAmount(vehicleCharge);
        booking.setTotalAmount(booking.getPlatformCharge().add(vehicleCharge));

        // Create Transaction record for Driver Earnings (Payment collected by driver)
        // Skipped for brevity, but analytics will read from remainingAmount

        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
}
