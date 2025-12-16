package com.skypay.booking.domain.exception;

/**
 * Exception thrown when a user does not have sufficient balance for a booking.
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(int userId, int requiredAmount, int currentBalance) {
        super(String.format("User #%d has insufficient balance. Required: %d, Current: %d",
                userId, requiredAmount, currentBalance));
    }
}