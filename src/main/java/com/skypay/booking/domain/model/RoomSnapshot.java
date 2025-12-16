package com.skypay.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable snapshot of room state at booking time.
 * This ensures that changes to the room after booking don't affect existing bookings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSnapshot {
    private int roomNumber;
    private RoomType roomType;
    private int pricePerNight;
}