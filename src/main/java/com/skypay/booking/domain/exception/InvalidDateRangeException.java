package com.skypay.booking.domain.exception;

import java.time.LocalDate;

/**
 * Exception thrown when the check-in date is not before the check-out date.
 */
public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException(String message) {
        super(message);
    }

    public InvalidDateRangeException(LocalDate checkIn, LocalDate checkOut) {
        super(String.format("Invalid date range: check-in (%s) must be before check-out (%s)",
                checkIn, checkOut));
    }
}