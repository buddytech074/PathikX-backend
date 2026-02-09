package com.vehiclebooking.backend.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.model.Payment;
import com.vehiclebooking.backend.model.enums.BookingStatus;
import com.vehiclebooking.backend.model.enums.PaymentStatus;
import com.vehiclebooking.backend.repository.BookingRepository;
import com.vehiclebooking.backend.repository.PaymentRepository;
import com.vehiclebooking.backend.service.PaymentService;
import com.vehiclebooking.backend.service.BookingService; // Add import
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.repository.TransactionRepository;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BookingService bookingService; // Injected service

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public Payment createPaymentOrder(Booking booking) throws Exception {
        try {
            // Initialize Razorpay client
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create order options
            JSONObject orderRequest = new JSONObject();
            // Convert platform charge to paise (INR smallest unit)
            int amountInPaise = booking.getPlatformCharge().multiply(BigDecimal.valueOf(100)).intValue();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_" + booking.getId());

            // Add notes for reference
            JSONObject notes = new JSONObject();
            notes.put("booking_id", booking.getId());

            String customerName = "Unknown";
            if (booking.getUser() != null && booking.getUser().getFullName() != null) {
                customerName = booking.getUser().getFullName();
            }
            notes.put("customer_name", customerName);

            String vTypeStr = "UNKNOWN";
            if (booking.getIsWedding() != null && booking.getIsWedding() && booking.getParentBooking() == null) {
                vTypeStr = "WEDDING_FLEET";
            } else if (booking.getVehicleType() != null) {
                vTypeStr = booking.getVehicleType().name();
            }
            notes.put("vehicle_type", vTypeStr);
            orderRequest.put("notes", notes);

            // Create order
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            // Save payment record
            Payment payment = Payment.builder()
                    .booking(booking)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .status(PaymentStatus.PENDING)
                    .amount(booking.getPlatformCharge())
                    .currency("INR")
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            System.out.println("========================================");
            System.out.println("💳 PAYMENT ORDER CREATED");
            System.out.println("Booking ID: " + booking.getId());
            System.out.println("Order ID: " + razorpayOrder.get("id"));
            System.out.println("Amount: ₹" + booking.getPlatformCharge());
            System.out.println("========================================");

            return savedPayment;

        } catch (RazorpayException e) {
            System.err.println("Error creating Razorpay order: " + e.getMessage());
            throw new Exception("Failed to create payment order: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            // Create signature verification string
            String payload = razorpayOrderId + "|" + razorpayPaymentId;

            // Generate HMAC SHA256 signature
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            String generatedSignature = hexString.toString();
            boolean isValid = generatedSignature.equals(razorpaySignature);

            System.out.println("========================================");
            System.out.println("🔐 PAYMENT SIGNATURE VERIFICATION");
            System.out.println("Order ID: " + razorpayOrderId);
            System.out.println("Payment ID: " + razorpayPaymentId);
            System.out.println("Signature Valid: " + isValid);
            System.out.println("========================================");

            return isValid;

        } catch (Exception e) {
            System.err.println("Error verifying signature: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public Payment handlePaymentSuccess(Long bookingId, String razorpayPaymentId, String razorpaySignature)
            throws Exception {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Verify signature (skip for test mode payments)
        boolean isTestPayment = razorpayPaymentId.startsWith("pay_test_");
        boolean isSignatureValid;

        if (isTestPayment) {
            System.out.println("⚠️ TEST MODE: Skipping signature verification for mock payment");
            isSignatureValid = true;
        } else {
            isSignatureValid = verifyPaymentSignature(
                    payment.getRazorpayOrderId(),
                    razorpayPaymentId,
                    razorpaySignature);
        }

        if (!isSignatureValid) {
            throw new Exception("Invalid payment signature");
        }

        // Update payment
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Update booking
        booking.setPaymentCompleted(true);
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);

        // If wedding parent, update all sub-bookings too
        if (booking.getIsWedding() != null && booking.getIsWedding() && booking.getParentBooking() == null) {
            java.util.List<Booking> subs = bookingRepository.findByParentBookingId(booking.getId());
            for (Booking sub : subs) {
                sub.setPaymentCompleted(true);
                sub.setStatus(BookingStatus.PENDING);
                bookingRepository.save(sub);
            }
        }

        System.out.println("========================================");
        System.out.println("✅ PAYMENT SUCCESSFUL");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("Payment ID: " + razorpayPaymentId);
        System.out.println("Amount: ₹" + payment.getAmount());
        System.out.println("Booking Status: " + booking.getStatus());
        System.out.println("========================================");

        // Notify drivers now that payment is confirmed
        bookingService.notifyDriversNewBooking(booking);

        return payment;
    }

    @Override
    @Transactional
    public Payment handlePaymentFailure(Long bookingId, String errorReason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Update payment status
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorReason(errorReason);
        paymentRepository.save(payment);

        System.out.println("========================================");
        System.out.println("❌ PAYMENT FAILED");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("Reason: " + errorReason);
        System.out.println("========================================");

        return payment;
    }

    @Override
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId).orElse(null);
    }

    @Override
    @Transactional
    public Payment createWalletTopUpOrder(Long userId, BigDecimal amount) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "wallet_" + userId + "_" + System.currentTimeMillis());

            JSONObject notes = new JSONObject();
            notes.put("user_id", userId);
            notes.put("type", "WALLET_TOPUP");
            orderRequest.put("notes", notes);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            Payment payment = Payment.builder()
                    .user(user)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .status(PaymentStatus.PENDING)
                    .amount(amount)
                    .currency("INR")
                    .build();

            return paymentRepository.save(payment);
        } catch (RazorpayException e) {
            throw new Exception("Failed to create wallet top-up order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Payment handleWalletTopUpSuccess(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature)
            throws Exception {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        User user = payment.getUser();
        if (user == null) {
            throw new Exception("No user associated with this payment");
        }

        // Verify signature
        if (!razorpayPaymentId.startsWith("pay_test_")) {
            boolean isValid = verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            if (!isValid)
                throw new Exception("Invalid payment signature");
        }

        // Update payment
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Update wallet balance
        BigDecimal currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
        user.setWalletBalance(currentBalance.add(payment.getAmount()));
        userRepository.save(user);

        // Record transaction
        Transaction creditTx = Transaction.builder()
                .user(user)
                .amount(payment.getAmount())
                .type("DRIVER")
                .transactionCategory("CREDIT")
                .status(PaymentStatus.SUCCESS)
                .description("Wallet top-up via Razorpay")
                .transactionId(razorpayPaymentId)
                .build();
        transactionRepository.save(creditTx);

        return payment;
    }
}
