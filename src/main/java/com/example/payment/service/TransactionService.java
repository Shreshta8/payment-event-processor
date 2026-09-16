package com.example.payment.service;

import com.example.payment.dto.TransactionRequest;
import com.example.payment.dto.TransactionResponse;
import com.example.payment.entity.Transaction;
import com.example.payment.entity.Wallet;
import com.example.payment.repository.TransactionRepository;
import com.example.payment.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(WalletRepository walletRepository,
                              TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {

        /*
         * Lock the wallet first.
         *
         * This prevents two concurrent debit requests for the same
         * wallet from reading and modifying the balance at the same time.
         */
        Wallet wallet = walletRepository
                .findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Wallet not found"));

        /*
         * Check idempotency AFTER acquiring the wallet lock.
         *
         * This is important for concurrent duplicate requests.
         * The first request can save the transaction while holding
         * the lock. Later requests will then see that it already exists.
         */
        if (transactionRepository.existsById(request.getTransactionId())) {

            return new TransactionResponse(
                    request.getTransactionId(),
                    "DUPLICATE",
                    wallet.getBalance(),
                    "Transaction has already been processed"
            );
        }

        /*
         * Validate the transaction amount.
         */
        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Transaction amount must be greater than zero");
        }

        /*
         * Currently the assignment specifies DEBIT transactions.
         */
        if (request.getType() == null) {
            throw new IllegalArgumentException(
                    "Transaction type is required");
        }

        /*
         * Check whether the wallet has enough money.
         */
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {

            return new TransactionResponse(
                    request.getTransactionId(),
                    "INSUFFICIENT_FUNDS",
                    wallet.getBalance(),
                    "Insufficient wallet balance"
            );
        }

        /*
         * Deduct the amount from the wallet.
         */
        BigDecimal newBalance =
                wallet.getBalance().subtract(request.getAmount());

        wallet.setBalance(newBalance);

        /*
         * Save the wallet update.
         */
        walletRepository.save(wallet);

        /*
         * Record the transaction.
         */
        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getUserId(),
                request.getAmount(),
                request.getType()
        );

        transactionRepository.save(transaction);

        /*
         * Because this method is @Transactional,
         * the wallet update and transaction record are committed
         * together.
         */
        return new TransactionResponse(
                request.getTransactionId(),
                "SUCCESS",
                newBalance,
                "Transaction processed successfully"
        );
    }
}