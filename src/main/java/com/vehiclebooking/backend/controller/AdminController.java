package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.enums.Role;
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/drivers/pending")
    public ResponseEntity<ApiResponse<List<User>>> getPendingDrivers() {
        // Assuming pending means ROLE=DRIVER and isVerified=false
        // You might want to filter more carefully
        List<User> drivers = userRepository.findByRoleAndIsVerifiedFalse(Role.DRIVER);
        return ResponseEntity.ok(ApiResponse.success(drivers, "Pending drivers fetched"));
    }

    @PostMapping("/drivers/{id}/verify")
    public ResponseEntity<ApiResponse<User>> verifyDriver(@PathVariable Long id) {
        User driver = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        driver.setVerified(true);
        userRepository.save(driver);

        return ResponseEntity.ok(ApiResponse.success(driver, "Driver verified successfully"));
    }
}
