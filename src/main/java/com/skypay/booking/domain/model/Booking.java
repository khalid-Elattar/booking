package com.skypay.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Booking entity storing SNAPSHOTS of room and user data at booking time.
 * This ensures that changes to room price or user balance after booking
 * don't affect the booking record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private int bookingId;
    private RoomSnapshot roomSnapshot;
    private UserSnapshot userSnapshot;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int totalCost;

    /**
     * Calculates the number of nights for this booking.
     *
     * @return number of nights between check-in and check-out
     */
    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Checks if this booking overlaps with the given date range.
     * Bookings overlap if their date ranges intersect.
     *
     * @param newCheckIn the check-in date to compare
     * @param newCheckOut the check-out date to compare
     * @return true if there's an overlap, false otherwise
     */
    public boolean overlaps(LocalDate newCheckIn, LocalDate newCheckOut) {
        // Two date ranges overlap if:
        // - The new check-in is before this booking's check-out, AND
        // - The new check-out is after this booking's check-in
        return newCheckIn.isBefore(this.checkOut) && newCheckOut.isAfter(this.checkIn);
    }
}