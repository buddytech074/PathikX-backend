package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.dto.AuthRequest;
import com.vehiclebooking.backend.dto.SaveOtpRequest;
import com.vehiclebooking.backend.dto.VerifyOtpRequest;
import com.vehiclebooking.backend.dto.DriverRegistrationRequest;
import com.vehiclebooking.backend.model.User;

public interface AuthService {
    String sendOtp(AuthRequest request);

    User verifyOtp(VerifyOtpRequest request);

    User loginWithPassword(AuthRequest request);

    User register(AuthRequest request);

    String saveFirebaseOtp(SaveOtpRequest request);

    User googleLogin(String idToken);

    User updatePhoneNumber(Long userId, String phoneNumber);

    User registerDriver(DriverRegistrationRequest request);

    User upgradeToDriver(DriverRegistrationRequest request);

    User getUserById(Long userId);
}
