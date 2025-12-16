package com.skypay.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable snapshot of user state at booking time.
 * This preserves the user's balance at the time of booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshot {
    private int userId;
    private int balanceAtBooking;
}