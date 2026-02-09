package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.model.Payment;
import com.vehiclebooking.backend.repository.BookingRepository;
import com.vehiclebooking.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    /**
     * Create payment order for a booking
     * POST /api/payments/create-order/{bookingId}
     */
    @PostMapping("/create-order/{bookingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentOrder(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            if (!booking.getPaymentRequired()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>builder()
                                .success(false)
                                .message("Payment not required for this booking")
                                .build());
            }

            Payment payment = paymentService.createPaymentOrder(booking);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", payment.getRazorpayOrderId());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("bookingId", booking.getId());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Payment order created successfully")
                    .data(response)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Failed to create payment order: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Verify payment after successful transaction
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Payment>> verifyPayment(@RequestBody Map<String, String> paymentData) {
        try {
            Long bookingId = Long.parseLong(paymentData.get("bookingId"));
            String razorpayPaymentId = paymentData.get("razorpayPaymentId");
            String razorpaySignature = paymentData.get("razorpaySignature");

            Payment payment = paymentService.handlePaymentSuccess(bookingId, razorpayPaymentId, razorpaySignature);

            return ResponseEntity.ok(ApiResponse.<Payment>builder()
                    .success(true)
                    .message("Payment verified successfully")
                    .data(payment)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Payment>builder()
                            .success(false)
                            .message("Payment verification failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Handle payment failure
     * POST /api/payments/failure
     */
    @PostMapping("/failure")
    public ResponseEntity<ApiResponse<Payment>> handlePaymentFailure(@RequestBody Map<String, String> failureData) {
        try {
            Long bookingId = Long.parseLong(failureData.get("bookingId"));
            String errorReason = failureData.getOrDefault("error", "Payment failed");

            Payment payment = paymentService.handlePaymentFailure(bookingId, errorReason);

            return ResponseEntity.ok(ApiResponse.<Payment>builder()
                    .success(true)
                    .message("Payment failure recorded")
                    .data(payment)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Payment>builder()
                            .success(false)
                            .message("Failed to record payment failure: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get payment details for a booking
     * GET /api/payments/booking/{bookingId}
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByBooking(@PathVariable Long bookingId) {
        try {
            Payment payment = paymentService.getPaymentByBookingId(bookingId);

            if (payment == null) {
                return ResponseEntity.ok(ApiResponse.<Payment>builder()
                        .success(true)
                        .message("No payment found for this booking")
                        .data(null)
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<Payment>builder()
                    .success(true)
                    .message("Payment details retrieved")
                    .data(payment)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Payment>builder()
                            .success(false)
                            .message("Failed to retrieve payment: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Razorpay webhook endpoint (for server-side payment verification)
     * POST /api/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            // Log webhook event
            System.out.println("========================================");
            System.out.println("📬 RAZORPAY WEBHOOK RECEIVED");
            System.out.println("Event: " + webhookData.get("event"));
            System.out.println("Data: " + webhookData);
            System.out.println("========================================");

            // TODO: Implement webhook signature verification
            // TODO: Process different webhook events (payment.captured, payment.failed,
            // etc.)

            return ResponseEntity.ok("Webhook received");

        } catch (Exception e) {
            System.err.println("Webhook processing error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }

    /**
     * Create payment order for wallet top-up
     * POST /api/payments/wallet/create-order
     */
    @PostMapping("/wallet/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createWalletTopUpOrder(
            @RequestBody Map<String, Object> data) {
        try {
            Long userId = Long.parseLong(data.get("userId").toString());
            java.math.BigDecimal amount = new java.math.BigDecimal(data.get("amount").toString());

            Payment payment = paymentService.createWalletTopUpOrder(userId, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", payment.getRazorpayOrderId());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("userId", userId);

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Wallet top-up order created successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Failed to create wallet order: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Verify wallet top-up payment
     * POST /api/payments/wallet/verify
     */
    @PostMapping("/wallet/verify")
    public ResponseEntity<ApiResponse<Payment>> verifyWalletTopUp(@RequestBody Map<String, String> data) {
        try {
            String razorpayOrderId = data.get("razorpayOrderId");
            String razorpayPaymentId = data.get("razorpayPaymentId");
            String razorpaySignature = data.get("razorpaySignature");

            Payment payment = paymentService.handleWalletTopUpSuccess(razorpayOrderId, razorpayPaymentId,
                    razorpaySignature);

            return ResponseEntity.ok(ApiResponse.<Payment>builder()
                    .success(true)
                    .message("Wallet topped up successfully")
                    .data(payment)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Payment>builder()
                            .success(false)
                            .message("Wallet top-up verification failed: " + e.getMessage())
                            .build());
        }
    }
}
