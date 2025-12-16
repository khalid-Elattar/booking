package com.skypay.booking.domain.service;

import com.skypay.booking.domain.exception.*;
import com.skypay.booking.domain.model.*;
import com.skypay.booking.infrastructure.repository.InMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

/**
 * Hotel Service managing rooms, users, and bookings.
 * Implements the specified interface with exact method signatures.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HotelService {
    public ArrayList<Room> rooms;
    public ArrayList<User> users;

    private final InMemoryStore store;

    /**
     * Creates a new room or updates an existing room.
     * IMPORTANT: Updates to existing rooms do NOT affect previously created bookings.
     *
     * @param roomNumber the room number
     * @param roomType the room type
     * @param roomPricePerNight the price per night
     */
    public void setRoom(int roomNumber, RoomType roomType, int roomPricePerNight) {
        Optional<Room> existingRoom = store.findRoomByNumber(roomNumber);

        if (existingRoom.isPresent()) {
            // Update existing room
            Room room = existingRoom.get();
            log.info("Updating Room #{}: {} -> {}, Price: {} -> {}",
                    roomNumber, room.getRoomType(), roomType,
                    room.getPricePerNight(), roomPricePerNight);
            room.setRoomType(roomType);
            room.setPricePerNight(roomPricePerNight);
        } else {
            // Create new room
            Room newRoom = Room.builder()
                    .roomNumber(roomNumber)
                    .roomType(roomType)
                    .pricePerNight(roomPricePerNight)
                    .build();
            store.addRoom(newRoom);
            log.info("Created Room #{}: {} at {} per night", roomNumber, roomType, roomPricePerNight);
        }

        // Update the public ArrayList reference
        rooms = store.getAllRooms();
    }

    /**
     * Books a room for a user for the specified date range.
     * Validates:
     * - User exists and has sufficient balance
     * - Room exists and is available for the period
     * - Check-in is before check-out
     *
     * @param userId the user ID
     * @param roomNumber the room number
     * @param checkIn the check-in date
     * @param checkOut the check-out date
     */
    public void bookRoom(int userId, int roomNumber, Date checkIn, Date checkOut) {
        // Convert Date to LocalDate
        LocalDate checkInDate = convertToLocalDate(checkIn);
        LocalDate checkOutDate = convertToLocalDate(checkOut);

        try {
            // Validate date range
            if (!checkInDate.isBefore(checkOutDate)) {
                throw new InvalidDateRangeException(checkInDate, checkOutDate);
            }

            // Find user
            User user = store.findUserById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            // Find room
            Room room = store.findRoomByNumber(roomNumber)
                    .orElseThrow(() -> new RoomNotFoundException(roomNumber));

            // Calculate total cost
            long numberOfNights = java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            int totalCost = (int) (numberOfNights * room.getPricePerNight());

            // Validate sufficient balance
            if (user.getBalance() < totalCost) {
                throw new InsufficientBalanceException(userId, totalCost, user.getBalance());
            }

            // Check room availability (no overlapping bookings)
            boolean isRoomAvailable = store.getAllBookings().stream()
                    .filter(booking -> booking.getRoomSnapshot().getRoomNumber() == roomNumber)
                    .noneMatch(booking -> booking.overlaps(checkInDate, checkOutDate));

            if (!isRoomAvailable) {
                throw new RoomNotAvailableException(roomNumber, checkInDate, checkOutDate);
            }

            // Create snapshots (capturing current state)
            RoomSnapshot roomSnapshot = room.createSnapshot();
            UserSnapshot userSnapshot = user.createSnapshot();

            // Deduct balance
            user.deductBalance(totalCost);

            // Create booking
            Booking booking = Booking.builder()
                    .bookingId(store.getNextBookingId())
                    .roomSnapshot(roomSnapshot)
                    .userSnapshot(userSnapshot)
                    .checkIn(checkInDate)
                    .checkOut(checkOutDate)
                    .totalCost(totalCost)
                    .build();

            store.addBooking(booking);

            log.info("SUCCESS: Booking #{} created - User #{} booked Room #{} from {} to {} for {} (Balance: {} -> {})",
                    booking.getBookingId(), userId, roomNumber, checkInDate, checkOutDate,
                    totalCost, userSnapshot.getBalanceAtBooking(), user.getBalance());

        } catch (InsufficientBalanceException e) {
            log.error("FAILED: Booking failed - {}", e.getMessage());
            throw e;
        } catch (RoomNotAvailableException e) {
            log.error("FAILED: Booking failed - {}", e.getMessage());
            throw e;
        } catch (InvalidDateRangeException e) {
            log.error("FAILED: Booking failed - {}", e.getMessage());
            throw e;
        } catch (UserNotFoundException | RoomNotFoundException e) {
            log.error("FAILED: Booking failed - {}", e.getMessage());
            throw e;
        }

        // Update the public ArrayList reference
        users = store.getAllUsers();
    }

    /**
     * Prints all rooms and bookings (newest to oldest).
     */
    public void printAll() {
        System.out.println("\n=== ROOMS (newest to oldest) ===");
        ArrayList<Room> allRooms = new ArrayList<>(store.getAllRooms());
        Collections.reverse(allRooms);
        for (Room room : allRooms) {
            System.out.printf("Room #%d | Type: %s | Price/Night: %d%n",
                    room.getRoomNumber(),
                    room.getRoomType(),
                    room.getPricePerNight());
        }

        System.out.println("\n=== BOOKINGS (newest to oldest) ===");
        ArrayList<Booking> allBookings = new ArrayList<>(store.getAllBookings());
        Collections.reverse(allBookings);
        for (Booking booking : allBookings) {
            RoomSnapshot roomSnap = booking.getRoomSnapshot();
            UserSnapshot userSnap = booking.getUserSnapshot();
            System.out.printf("Booking #%d | Room: #%d (%s, %d/night) | User: #%d (balance at booking: %d) | Check-in: %s | Check-out: %s | Total: %d%n",
                    booking.getBookingId(),
                    roomSnap.getRoomNumber(),
                    roomSnap.getRoomType(),
                    roomSnap.getPricePerNight(),
                    userSnap.getUserId(),
                    userSnap.getBalanceAtBooking(),
                    booking.getCheckIn(),
                    booking.getCheckOut(),
                    booking.getTotalCost());
        }
    }

    /**
     * Creates a new user or updates an existing user's balance.
     *
     * @param userId the user ID
     * @param balance the balance
     */
    public void setUser(int userId, int balance) {
        Optional<User> existingUser = store.findUserById(userId);

        if (existingUser.isPresent()) {
            // Update existing user
            User user = existingUser.get();
            log.info("Updating User #{}: Balance {} -> {}", userId, user.getBalance(), balance);
            user.setBalance(balance);
        } else {
            // Create new user
            User newUser = User.builder()
                    .userId(userId)
                    .balance(balance)
                    .build();
            store.addUser(newUser);
            log.info("Created User #{} with balance {}", userId, balance);
        }

        // Update the public ArrayList reference
        users = store.getAllUsers();
    }

    /**
     * Prints all users (newest to oldest).
     */
    public void printAllUsers() {
        System.out.println("\n=== USERS (newest to oldest) ===");
        ArrayList<User> allUsers = new ArrayList<>(store.getAllUsers());
        Collections.reverse(allUsers);
        for (User user : allUsers) {
            System.out.printf("User #%d | Balance: %d%n",
                    user.getUserId(),
                    user.getBalance());
        }
    }

    /**
     * Converts java.util.Date to LocalDate.
     *
     * @param date the Date to convert
     * @return LocalDate representation
     */
    private LocalDate convertToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}