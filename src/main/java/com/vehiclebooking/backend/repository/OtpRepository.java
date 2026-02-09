package com.vehiclebooking.backend.repository;

import com.vehiclebooking.backend.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByPhoneNumberAndIsUsedFalseOrderByCreatedAtDesc(String phoneNumber);

    List<Otp> findByPhoneNumberAndIsUsedFalse(String phoneNumber);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
