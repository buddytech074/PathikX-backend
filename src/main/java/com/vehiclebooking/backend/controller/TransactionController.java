package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.model.Transaction;
import com.vehiclebooking.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getUserTransactions(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(ApiResponse.<List<Transaction>>builder()
                    .success(true)
                    .message("Transactions retrieved successfully")
                    .data(transactions)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<Transaction>>builder()
                            .success(false)
                            .message("Failed to fetch transactions")
                            .build());
        }
    }
}
