package com.skypay.booking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Room entity representing a hotel room.
 * Two rooms can have the same type but different prices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private int roomNumber;
    private RoomType roomType;
    private int pricePerNight;

    /**
     * Creates a snapshot of the current room state.
     *
     * @return RoomSnapshot capturing current state
     */
    public RoomSnapshot createSnapshot() {
        return RoomSnapshot.builder()
                .roomNumber(this.roomNumber)
                .roomType(this.roomType)
                .pricePerNight(this.pricePerNight)
                .build();
    }
}