package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.AdminStatsDTO;
import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.model.Complaint;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.Voucher;
import com.vehiclebooking.backend.model.enums.BookingStatus;
import com.vehiclebooking.backend.model.enums.ComplaintStatus;
import com.vehiclebooking.backend.model.enums.Role;
import com.vehiclebooking.backend.repository.BookingRepository;
import com.vehiclebooking.backend.repository.ComplaintRepository;
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.repository.VoucherRepository;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ComplaintRepository complaintRepository;
    private final VoucherRepository voucherRepository;

    // --- STATS ---
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDTO>> getStats() {
        long totalUsers = userRepository.countByRole(Role.CUSTOMER);
        long activeDrivers = userRepository.countByRole(Role.DRIVER); // For now filtering by role only
        long totalBookings = bookingRepository.count();
        BigDecimal totalRevenue = bookingRepository.sumTotalAmountByStatus(BookingStatus.COMPLETED);

        if (totalRevenue == null)
            totalRevenue = BigDecimal.ZERO;

        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeDrivers(activeDrivers)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats, "Stats fetched successfully"));
    }

    // --- USERS & DRIVERS ---
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        // In real app, use pagination
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(users, "Users fetched"));
    }

    @PostMapping("/users/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long id) {
        // Logic to block user (add isBlocked to User model later if not present,
        // assuming just placeholder)
        // For now, let's just return success
        return ResponseEntity.ok(ApiResponse.success(null, "User blocked successfully"));
    }

    @GetMapping("/drivers/pending")
    public ResponseEntity<ApiResponse<List<User>>> getPendingDrivers() {
        List<User> drivers = userRepository.findByRoleAndIsVerifiedFalse(Role.DRIVER);
        return ResponseEntity.ok(ApiResponse.success(drivers, "Pending drivers fetched"));
    }

    @PostMapping("/drivers/{id}/verify")
    public ResponseEntity<ApiResponse<User>> verifyDriver(@PathVariable Long id) {
        User driver = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        driver.setVerified(true);
        userRepository.save(driver);
        return ResponseEntity.ok(ApiResponse.success(driver, "Driver verified"));
    }

    // --- BOOKINGS ---
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.success(bookingRepository.findAll(), "Bookings fetched"));
    }

    // --- COMPLAINTS ---
    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getAllComplaints() {
        return ResponseEntity.ok(ApiResponse.success(complaintRepository.findAll(), "Complaints fetched"));
    }

    @PostMapping("/complaints/{id}/resolve")
    public ResponseEntity<ApiResponse<Complaint>> resolveComplaint(@PathVariable Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolvedAt(LocalDateTime.now());
        complaintRepository.save(complaint);
        return ResponseEntity.ok(ApiResponse.success(complaint, "Complaint resolved"));
    }

    // --- VOUCHERS ---
    @GetMapping("/vouchers")
    public ResponseEntity<ApiResponse<List<Voucher>>> getAllVouchers() {
        return ResponseEntity.ok(ApiResponse.success(voucherRepository.findAll(), "Vouchers fetched"));
    }

    @PostMapping("/vouchers")
    public ResponseEntity<ApiResponse<Voucher>> createVoucher(@RequestBody Voucher voucher) {
        Voucher saved = voucherRepository.save(voucher);
        return ResponseEntity.ok(ApiResponse.success(saved, "Voucher created"));
    }

    @DeleteMapping("/vouchers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Long id) {
        voucherRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Voucher deleted"));
    }
}
