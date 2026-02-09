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

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<Booking>> getBookingById(@PathVariable Long bookingId) {
        Booking booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking fetched successfully"));
    }

    @GetMapping("/driver")
    public ResponseEntity<ApiResponse<List<Booking>>> getDriverBookings(@RequestParam Long driverId) {
        List<Booking> bookings = bookingService.getDriverBookings(driverId);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Driver Bookings Fetched"));
    }

    @GetMapping("/pending-for-driver")
    public ResponseEntity<ApiResponse<List<Booking>>> getPendingBookingsForDriver(@RequestParam Long driverId) {
        List<Booking> bookings = bookingService.getPendingBookingsForDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Pending bookings fetched"));
    }

    @PostMapping("/accept-by-driver")
    public ResponseEntity<ApiResponse<Booking>> acceptBookingByDriver(
            @RequestParam Long bookingId,
            @RequestParam Long driverId) {
        Booking booking = bookingService.acceptBookingByDriver(bookingId, driverId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking accepted successfully"));
    }

    @PostMapping("/cancel-accepted")
    public ResponseEntity<ApiResponse<Booking>> cancelAcceptedBooking(
            @RequestParam Long bookingId,
            @RequestParam Long driverId) {
        Booking booking = bookingService.cancelAcceptedBooking(bookingId, driverId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking cancelled and re-notified to drivers"));
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<Booking>> rejectBooking(@RequestParam Long bookingId) {
        Booking booking = bookingService.rejectBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking rejected"));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Booking>> startRide(
            @RequestParam Long bookingId,
            @RequestParam String otpInput) {
        Booking booking = bookingService.startRide(bookingId, otpInput);
        return ResponseEntity.ok(ApiResponse.success(booking, "Ride started"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Booking>> getActiveBooking(@RequestParam Long userId) {
        Booking activeBooking = bookingService.getActiveBookingForUser(userId);
        if (activeBooking != null) {
            return ResponseEntity.ok(ApiResponse.success(activeBooking, "Active booking found"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "No active booking"));
    }

    @GetMapping("/driver-active")
    public ResponseEntity<ApiResponse<List<Booking>>> getActiveBookingsForDriver(@RequestParam Long driverId) {
        List<Booking> activeBookings = bookingService.getActiveBookingsForDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(activeBookings, "Active bookings found"));
    }

    @GetMapping("/driver-tasks")
    public ResponseEntity<ApiResponse<List<com.vehiclebooking.backend.dto.DriverTaskDto>>> getDriverTasks(
            @RequestParam Long driverId) {
        List<com.vehiclebooking.backend.dto.DriverTaskDto> tasks = bookingService.getDriverTasks(driverId);
        return ResponseEntity.ok(ApiResponse.success(tasks, "Driver tasks fetched"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(@RequestParam Long bookingId) {
        Booking booking = bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking cancelled"));
    }

    @GetMapping("/sub-bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> getSubBookings(@RequestParam Long parentId) {
        List<Booking> subBookings = bookingService.getSubBookings(parentId);
        return ResponseEntity.ok(ApiResponse.success(subBookings, "Sub-bookings fetched"));
    }

    // Passenger verification endpoints for shared routes
    @PostMapping("/verify-pickup")
    public ResponseEntity<ApiResponse<Booking>> verifyPickupOtp(
            @RequestParam Long bookingId,
            @RequestParam Long passengerId,
            @RequestParam String otpInput) {
        Booking booking = bookingService.verifyPickupOtp(bookingId, passengerId, otpInput);
        return ResponseEntity.ok(ApiResponse.success(booking, "Passenger pickup verified"));
    }

    @PostMapping("/verify-drop")
    public ResponseEntity<ApiResponse<Booking>> verifyDropOtp(
            @RequestParam Long bookingId,
            @RequestParam Long passengerId,
            @RequestParam String otpInput) {
        Booking booking = bookingService.verifyDropOtp(bookingId, passengerId, otpInput);
        return ResponseEntity.ok(ApiResponse.success(booking, "Passenger drop verified"));
    }

    @GetMapping("/passenger-verifications")
    public ResponseEntity<ApiResponse<List<com.vehiclebooking.backend.dto.PassengerVerificationDto>>> getPassengerVerifications(
            @RequestParam Long bookingId) {
        List<com.vehiclebooking.backend.dto.PassengerVerificationDto> verifications = bookingService
                .getPassengerVerifications(bookingId);
        return ResponseEntity.ok(ApiResponse.success(verifications, "Passenger verifications fetched"));
    }

    @GetMapping("/driver-earnings")
    public ResponseEntity<ApiResponse<com.vehiclebooking.backend.dto.DriverEarningsDto>> getDriverEarnings(
            @RequestParam Long driverId) {
        com.vehiclebooking.backend.dto.DriverEarningsDto earnings = bookingService.getDriverEarnings(driverId);
        return ResponseEntity.ok(ApiResponse.success(earnings, "Driver earnings fetched"));
    }
}
