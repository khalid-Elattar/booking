package com.skypay.booking.domain.exception;

import java.time.LocalDate;

/**
 * Exception thrown when a room is not available for the requested date range.
 */
public class RoomNotAvailableException extends RuntimeException {
    public RoomNotAvailableException(String message) {
        super(message);
    }

    public RoomNotAvailableException(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        super(String.format("Room #%d is not available for the period from %s to %s",
                roomNumber, checkIn, checkOut));
    }
}