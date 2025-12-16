package com.skypay.booking.infrastructure.repository;

import com.skypay.booking.domain.model.Booking;
import com.skypay.booking.domain.model.Room;
import com.skypay.booking.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

/**
 * In-memory storage for rooms, users, and bookings using ArrayLists.
 * Maintains insertion order for newest-to-oldest printing.
 */
@Component
public class InMemoryStore {
    private final ArrayList<Room> rooms = new ArrayList<>();
    private final ArrayList<User> users = new ArrayList<>();
    private final ArrayList<Booking> bookings = new ArrayList<>();

    // Room operations
    public void addRoom(Room room) {
        rooms.add(room);
    }

    public Optional<Room> findRoomByNumber(int roomNumber) {
        return rooms.stream()
                .filter(room -> room.getRoomNumber() == roomNumber)
                .findFirst();
    }

    public ArrayList<Room> getAllRooms() {
        return rooms;
    }

    // User operations
    public void addUser(User user) {
        users.add(user);
    }

    public Optional<User> findUserById(int userId) {
        return users.stream()
                .filter(user -> user.getUserId() == userId)
                .findFirst();
    }

    public ArrayList<User> getAllUsers() {
        return users;
    }

    // Booking operations
    public void addBooking(Booking booking) {
        bookings.add(booking);
    }

    public ArrayList<Booking> getAllBookings() {
        return bookings;
    }

    /**
     * Gets the next available booking ID.
     *
     * @return the next booking ID
     */
    public int getNextBookingId() {
        return bookings.size() + 1;
    }
}