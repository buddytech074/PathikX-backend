package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.dto.AuthRequest;
import com.vehiclebooking.backend.dto.DriverRegistrationRequest;
import com.vehiclebooking.backend.dto.GoogleLoginRequest;
import com.vehiclebooking.backend.dto.SaveOtpRequest;
import com.vehiclebooking.backend.dto.VerifyOtpRequest;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow Expo app to connect
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestBody AuthRequest request) {
        String result = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(result, "OTP Sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<User>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        User user = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(user, "Verification Successful"));
    }

    @PostMapping("/login-password")
    public ResponseEntity<ApiResponse<User>> loginWithPassword(@RequestBody AuthRequest request) {
        User user = authService.loginWithPassword(request);
        return ResponseEntity.ok(ApiResponse.success(user, "Login Successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody AuthRequest request) {
        User user = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(user, "Registration Successful"));
    }

    @PostMapping("/save-firebase-otp")
    public ResponseEntity<ApiResponse<String>> saveFirebaseOtp(@RequestBody SaveOtpRequest request) {
        String result = authService.saveFirebaseOtp(request);
        return ResponseEntity.ok(ApiResponse.success(result, "OTP Saved"));
    }

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<User>> googleLogin(@RequestBody GoogleLoginRequest request) {
        User user = authService.googleLogin(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success(user, "Google Login Successful"));
    }

    @PutMapping("/update-phone")
    public ResponseEntity<ApiResponse<User>> updatePhoneNumber(
            @RequestParam Long userId,
            @RequestParam String phoneNumber) {
        User user = authService.updatePhoneNumber(userId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(user, "Phone number updated successfully"));
    }

    @PostMapping("/register-driver")
    public ResponseEntity<ApiResponse<User>> registerDriver(@RequestBody DriverRegistrationRequest request) {
        User driver = authService.registerDriver(request);
        return ResponseEntity.ok(ApiResponse.success(driver, "Driver registered successfully"));
    }

    @PostMapping("/upgrade-to-driver")
    public ResponseEntity<ApiResponse<User>> upgradeToDriver(@RequestBody DriverRegistrationRequest request) {
        User driver = authService.upgradeToDriver(request);
        return ResponseEntity.ok(ApiResponse.success(driver, "User upgraded to driver successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(@RequestParam Long userId) {
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "Profile retrieved successfully"));
    }
}
