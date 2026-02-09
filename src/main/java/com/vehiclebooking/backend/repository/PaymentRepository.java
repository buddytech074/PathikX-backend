package com.vehiclebooking.backend.repository;

import com.vehiclebooking.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find the most recent payment for a booking (handles multiple test payments)
    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
}
