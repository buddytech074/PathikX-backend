package com.vehiclebooking.backend.service.impl;

import com.vehiclebooking.backend.dto.AuthRequest;
import com.vehiclebooking.backend.dto.VerifyOtpRequest;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.enums.Role;
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String sendOtp(AuthRequest request) {
        // Mock OTP generation
        String otp = "123456";
        System.out.println("OTP for " + request.getPhoneNumber() + ": " + otp);
        return "OTP sent successfully";
    }

    @Override
    public User verifyOtp(VerifyOtpRequest request) {
        // Mock verification - in real app, verify against Redis/Database
        if ("123456".equals(request.getOtp())) {
            Optional<User> userOpt = userRepository.findByPhoneNumber(request.getPhoneNumber());
            if (userOpt.isPresent()) {
                return userOpt.get();
            } else {
                // Register new user
                User newUser = User.builder()
                        .phoneNumber(request.getPhoneNumber())
                        .role(Role.CUSTOMER) // Default
                        .build();
                return userRepository.save(newUser);
            }
        } else {
            throw new RuntimeException("Invalid OTP");
        }
    }

    @Override
    public User loginWithPassword(AuthRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }

    @Override
    public User register(AuthRequest request) {
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("User already exists with this phone number");
        }

        User newUser = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.CUSTOMER)
                .isVerified(true) // For now auto verify
                .build();

        return userRepository.save(newUser);
    }
}
