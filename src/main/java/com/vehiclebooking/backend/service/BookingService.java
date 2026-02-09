package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.dto.BookingDto;
import com.vehiclebooking.backend.model.Booking;
import java.util.List;

public interface BookingService {
    Booking createBooking(Long userId, BookingDto bookingDto);

    Booking acceptBooking(Long bookingId);

    Booking completeBooking(Long bookingId);

    List<Booking> getUserBookings(Long userId);

    Booking getBookingById(Long bookingId);

    List<Booking> getDriverBookings(Long driverId);

    List<Booking> getPendingBookingsForDriver(Long driverId);

    Booking acceptBookingByDriver(Long bookingId, Long driverId);

    Booking rejectBooking(Long bookingId);

    Booking startRide(Long bookingId, String otpInput);

    Booking getActiveBookingForUser(Long userId);

    List<Booking> getActiveBookingsForDriver(Long driverId);

    List<com.vehiclebooking.backend.dto.DriverTaskDto> getDriverTasks(Long driverId);

    void notifyDriversNewBooking(Booking booking);

    Booking cancelBooking(Long bookingId);

    Booking cancelAcceptedBooking(Long bookingId, Long driverId);

    List<Booking> getSubBookings(Long parentId);

    // Passenger verification methods for shared routes
    Booking verifyPickupOtp(Long bookingId, Long passengerId, String otpInput);

    Booking verifyDropOtp(Long bookingId, Long passengerId, String otpInput);

    List<com.vehiclebooking.backend.dto.PassengerVerificationDto> getPassengerVerifications(Long bookingId);

    com.vehiclebooking.backend.dto.DriverEarningsDto getDriverEarnings(Long driverId);
}
