package com.skypay.booking.domain.exception;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(int userId) {
        super(String.format("User #%d not found", userId));
    }
}