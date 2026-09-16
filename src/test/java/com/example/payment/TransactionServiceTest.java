package com.example.payment;

import com.example.payment.dto.TransactionRequest;
import com.example.payment.dto.TransactionResponse;
import com.example.payment.entity.Wallet;
import com.example.payment.enums.TransactionType;
import com.example.payment.repository.TransactionRepository;
import com.example.payment.repository.WalletRepository;
import com.example.payment.service.TransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {

        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        userId = UUID.randomUUID();

        Wallet wallet = new Wallet(
                userId,
                new BigDecimal("500.00")
        );

        walletRepository.save(wallet);
    }

    @Test
    @DisplayName("Processes a single valid debit transaction successfully.")
    void processesSingleValidDebitSuccessfully() {

        UUID transactionId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                transactionId,
                userId,
                new BigDecimal("100.00"),
                TransactionType.DEBIT
        );

        TransactionResponse response =
                transactionService.processTransaction(request);

        Assertions.assertEquals(
                "SUCCESS",
                response.getStatus()
        );

        Assertions.assertEquals(
                new BigDecimal("400.00"),
                response.getBalance()
        );

        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow();

        Assertions.assertEquals(
                new BigDecimal("400.00"),
                wallet.getBalance()
        );
    }

    @Test
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    void processesThreeIdenticalTransactionsOnlyOnce()
            throws InterruptedException {

        UUID transactionId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                transactionId,
                userId,
                new BigDecimal("100.00"),
                TransactionType.DEBIT
        );

        int numberOfRequests = 3;

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfRequests);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishedLatch =
                new CountDownLatch(numberOfRequests);

        List<TransactionResponse> responses =
                new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {

            executorService.submit(() -> {

                try {

                    startLatch.await();

                    TransactionResponse response =
                            transactionService.processTransaction(request);

                    synchronized (responses) {
                        responses.add(response);
                    }

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                } finally {

                    finishedLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean completed =
                finishedLatch.await(10, TimeUnit.SECONDS);

        executorService.shutdown();

        Assertions.assertTrue(completed);

        long successCount = responses.stream()
                .filter(response ->
                        "SUCCESS".equals(response.getStatus()))
                .count();

        Assertions.assertEquals(1, successCount);

        long duplicateCount = responses.stream()
                .filter(response ->
                        "DUPLICATE".equals(response.getStatus()))
                .count();

        Assertions.assertEquals(2, duplicateCount);

        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow();

        Assertions.assertEquals(
                new BigDecimal("400.00"),
                wallet.getBalance()
        );

        Assertions.assertEquals(
                1,
                transactionRepository.count()
        );
    }

    @Test
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
    void processesTenConcurrentDebitsWithCorrectBalance()
            throws InterruptedException {

        int numberOfRequests = 10;

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfRequests);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishedLatch =
                new CountDownLatch(numberOfRequests);

        List<TransactionResponse> responses =
                new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {

            UUID transactionId = UUID.randomUUID();

            TransactionRequest request =
                    new TransactionRequest(
                            transactionId,
                            userId,
                            new BigDecimal("100.00"),
                            TransactionType.DEBIT
                    );

            executorService.submit(() -> {

                try {

                    startLatch.await();

                    TransactionResponse response =
                            transactionService.processTransaction(request);

                    synchronized (responses) {
                        responses.add(response);
                    }

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                } finally {

                    finishedLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean completed =
                finishedLatch.await(10, TimeUnit.SECONDS);

        executorService.shutdown();

        Assertions.assertTrue(completed);

        long successCount = responses.stream()
                .filter(response ->
                        "SUCCESS".equals(response.getStatus()))
                .count();

        long insufficientFundsCount = responses.stream()
                .filter(response ->
                        "INSUFFICIENT_FUNDS"
                                .equals(response.getStatus()))
                .count();

        Assertions.assertEquals(5, successCount);

        Assertions.assertEquals(5, insufficientFundsCount);

        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow();

        Assertions.assertEquals(
                new BigDecimal("0.00"),
                wallet.getBalance()
        );

        Assertions.assertEquals(
                5,
                transactionRepository.count()
        );
    }
}