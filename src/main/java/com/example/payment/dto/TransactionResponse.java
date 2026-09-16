package com.example.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionResponse {

    private UUID transactionId;
    private String status;
    private BigDecimal balance;
    private String message;

    public TransactionResponse() {
    }

    public TransactionResponse(UUID transactionId,
                               String status,
                               BigDecimal balance,
                               String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.balance = balance;
        this.message = message;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}