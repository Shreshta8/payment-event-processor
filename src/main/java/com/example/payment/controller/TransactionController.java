package com.example.payment.controller;

import com.example.payment.dto.TransactionRequest;
import com.example.payment.dto.TransactionResponse;
import com.example.payment.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse response =
                transactionService.processTransaction(request);

        if ("DUPLICATE".equals(response.getStatus())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(response);
        }

        if ("INSUFFICIENT_FUNDS".equals(response.getStatus())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(response);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}