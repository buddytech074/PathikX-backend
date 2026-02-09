package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.model.Payment;

public interface PaymentService {

    /**
     * Create a Razorpay payment order for a booking
     * 
     * @param booking The booking for which to create payment order
     * @return Payment entity with order details
     */
    Payment createPaymentOrder(Booking booking) throws Exception;

    /**
     * Verify Razorpay payment signature
     * 
     * @param razorpayOrderId   Order ID from Razorpay
     * @param razorpayPaymentId Payment ID from Razorpay
     * @param razorpaySignature Signature from Razorpay
     * @return true if signature is valid
     */
    boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

    /**
     * Handle successful payment
     * 
     * @param bookingId         Booking ID
     * @param razorpayPaymentId Payment ID from Razorpay
     * @return Updated payment entity
     */
    Payment handlePaymentSuccess(Long bookingId, String razorpayPaymentId, String razorpaySignature) throws Exception;

    /**
     * Handle payment failure
     * 
     * @param bookingId   Booking ID
     * @param errorReason Reason for failure
     * @return Updated payment entity
     */
    Payment handlePaymentFailure(Long bookingId, String errorReason);

    /**
     * Get payment details for a booking
     * 
     * @param bookingId Booking ID
     * @return Payment entity if exists
     */
    Payment getPaymentByBookingId(Long bookingId);

    /**
     * Create a Razorpay payment order for wallet top-up
     * 
     * @param userId userId who is topping up
     * @param amount amount to top up
     * @return Payment entity with order details
     */
    Payment createWalletTopUpOrder(Long userId, java.math.BigDecimal amount) throws Exception;

    /**
     * Handle successful wallet top-up
     * 
     * @param razorpayOrderId   Order ID from Razorpay
     * @param razorpayPaymentId Payment ID from Razorpay
     * @param razorpaySignature Signature from Razorpay
     * @return Updated payment entity
     */
    Payment handleWalletTopUpSuccess(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature)
            throws Exception;
}
