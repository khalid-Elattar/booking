package com.skypay.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User entity representing a hotel guest.
 * Balance is deducted when booking is successful.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int userId;
    private int balance;

    /**
     * Deducts amount from user's balance.
     *
     * @param amount the amount to deduct
     * @throws IllegalArgumentException if amount would result in negative balance
     */
    public void deductBalance(int amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.balance -= amount;
    }

    /**
     * Creates a snapshot of the current user state.
     *
     * @return UserSnapshot capturing current state
     */
    public UserSnapshot createSnapshot() {
        return UserSnapshot.builder()
                .userId(this.userId)
                .balanceAtBooking(this.balance)
                .build();
    }
}