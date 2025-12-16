package com.skypay.booking.domain.exception;

/**
 * Exception thrown when a room is not found.
 */
public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String message) {
        super(message);
    }

    public RoomNotFoundException(int roomNumber) {
        super(String.format("Room #%d not found", roomNumber));
    }
}