package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.dto.AuthRequest;
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
}
