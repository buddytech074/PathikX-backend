package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.dto.BookingDto;
import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Booking>> createBooking(@RequestParam Long userId,
            @RequestBody BookingDto bookingDto) {
        Booking booking = bookingService.createBooking(userId, bookingDto);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking Created"));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Booking>> acceptBooking(@RequestParam Long bookingId) {
        Booking booking = bookingService.acceptBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking Accepted"));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Booking>> completeBooking(@RequestParam Long bookingId) {
        Booking booking = bookingService.completeBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking Completed"));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<Booking>>> getUserBookings(@RequestParam Long userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(ApiResponse.success(bookings, "User Bookings Fetched"));
    }

    @GetMapping("/driver")
    public ResponseEntity<ApiResponse<List<Booking>>> getDriverBookings(@RequestParam Long driverId) {
        List<Booking> bookings = bookingService.getDriverBookings(driverId);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Driver Bookings Fetched"));
    }
}
