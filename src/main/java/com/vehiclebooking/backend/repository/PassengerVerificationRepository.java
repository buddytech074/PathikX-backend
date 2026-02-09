package com.vehiclebooking.backend.repository;

import com.vehiclebooking.backend.model.PassengerVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerVerificationRepository extends JpaRepository<PassengerVerification, Long> {

    List<PassengerVerification> findByBookingId(Long bookingId);

    Optional<PassengerVerification> findByBookingIdAndPassengerId(Long bookingId, Long passengerId);

    List<PassengerVerification> findByBookingIdAndPickupVerifiedFalse(Long bookingId);

    List<PassengerVerification> findByBookingIdAndDropVerifiedFalse(Long bookingId);
}
