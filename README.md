# Idempotent Payment & Wallet Event Processor

A Java Spring Boot backend service that processes payment/wallet events safely, even when duplicate requests are received concurrently.

The project was developed as part of a Java Backend Intern assignment.

## Problem

Payment gateways may retry webhook requests when network issues occur. This can result in the same transaction being received multiple times at nearly the same time.

The system must ensure that:

- The same transaction is not processed more than once.
- Concurrent debit requests do not cause the wallet balance to become negative.
- Wallet updates and transaction records remain consistent.
- Concurrent requests are handled safely at the database level.

## Tech Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- JUnit 5
- Maven

## Architecture

```text
Client
   |
   v
Transaction Controller
   |
   v
Transaction Service
   |
   +----------------------+
   |                      |
   v                      v
Wallet Repository    Transaction Repository
   |                      |
   +----------+-----------+
              |
              v
          H2 Database
