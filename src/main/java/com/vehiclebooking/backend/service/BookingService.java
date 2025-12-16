package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.dto.BookingDto;
import com.vehiclebooking.backend.model.Booking;
import java.util.List;

public interface BookingService {
    Booking createBooking(Long userId, BookingDto bookingDto);

    Booking acceptBooking(Long bookingId);

    Booking completeBooking(Long bookingId);

    List<Booking> getUserBookings(Long userId);

    List<Booking> getDriverBookings(Long driverId);
}
