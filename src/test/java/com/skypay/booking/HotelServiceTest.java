package com.skypay.booking;

import com.skypay.booking.domain.exception.*;
import com.skypay.booking.domain.model.Booking;
import com.skypay.booking.domain.model.Room;
import com.skypay.booking.domain.model.RoomType;
import com.skypay.booking.domain.model.User;
import com.skypay.booking.domain.service.HotelService;
import com.skypay.booking.infrastructure.repository.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HotelServiceTest {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private InMemoryStore store;

    @BeforeEach
    void setUp() {
        // Clear all data before each test
        store.getAllRooms().clear();
        store.getAllUsers().clear();
        store.getAllBookings().clear();
    }

    // =====================================================
    // TEST GROUP 1: ROOM MANAGEMENT
    // =====================================================

    @Test
    @DisplayName("setRoom should create a new room when it doesn't exist")
    void setRoom_CreatesNewRoom_WhenRoomDoesNotExist() {
        // When
        hotelService.setRoom(1, RoomType.STANDARD, 1000);

        // Then
        Room room = store.findRoomByNumber(1).orElse(null);
        assertNotNull(room, "Room should be created");
        assertEquals(1, room.getRoomNumber());
        assertEquals(RoomType.STANDARD, room.getRoomType());
        assertEquals(1000, room.getPricePerNight());
    }

    @Test
    @DisplayName("setRoom should update existing room properties")
    void setRoom_UpdatesRoom_WhenRoomExists() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);

        // When
        hotelService.setRoom(1, RoomType.MASTER_SUITE, 5000);

        // Then
        Room room = store.findRoomByNumber(1).orElse(null);
        assertNotNull(room);
        assertEquals(RoomType.MASTER_SUITE, room.getRoomType());
        assertEquals(5000, room.getPricePerNight());
    }

    @Test
    @DisplayName("setRoom should NOT impact previous bookings (CRITICAL)")
    void setRoom_DoesNotImpactPreviousBookings() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 5000);
        hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8));

        // Get the booking
        Booking booking = store.getAllBookings().get(0);
        assertEquals(RoomType.STANDARD, booking.getRoomSnapshot().getRoomType());
        assertEquals(1000, booking.getRoomSnapshot().getPricePerNight());

        // When
        hotelService.setRoom(1, RoomType.MASTER_SUITE, 10000);

        // Then - Room is updated
        Room room = store.findRoomByNumber(1).orElse(null);
        assertEquals(RoomType.MASTER_SUITE, room.getRoomType());
        assertEquals(10000, room.getPricePerNight());

        // But booking still shows original values (snapshot preserved)
        assertEquals(RoomType.STANDARD, booking.getRoomSnapshot().getRoomType());
        assertEquals(1000, booking.getRoomSnapshot().getPricePerNight());
    }

    // =====================================================
    // TEST GROUP 2: USER MANAGEMENT
    // =====================================================

    @Test
    @DisplayName("setUser should create a new user when it doesn't exist")
    void setUser_CreatesNewUser_WhenUserDoesNotExist() {
        // When
        hotelService.setUser(1, 5000);

        // Then
        User user = store.findUserById(1).orElse(null);
        assertNotNull(user, "User should be created");
        assertEquals(1, user.getUserId());
        assertEquals(5000, user.getBalance());
    }

    @Test
    @DisplayName("setUser should update existing user balance")
    void setUser_UpdatesBalance_WhenUserExists() {
        // Given
        hotelService.setUser(1, 5000);

        // When
        hotelService.setUser(1, 10000);

        // Then
        User user = store.findUserById(1).orElse(null);
        assertNotNull(user);
        assertEquals(10000, user.getBalance());
    }

    // =====================================================
    // TEST GROUP 3: BOOKING VALIDATION
    // =====================================================

    @Test
    @DisplayName("bookRoom should FAIL when user has insufficient balance")
    void bookRoom_Fails_WhenInsufficientBalance() {
        // Given
        hotelService.setRoom(2, RoomType.JUNIOR_SUITE, 2000);
        hotelService.setUser(1, 5000);

        // When/Then
        assertThrows(InsufficientBalanceException.class, () ->
                hotelService.bookRoom(1, 2, date(2026, 6, 30), date(2026, 7, 7)));

        // Verify balance unchanged
        User user = store.findUserById(1).orElse(null);
        assertEquals(5000, user.getBalance());
    }

    @Test
    @DisplayName("bookRoom should FAIL when checkOut is before checkIn")
    void bookRoom_Fails_WhenInvalidDateRange() {
        // Given
        hotelService.setRoom(2, RoomType.JUNIOR_SUITE, 2000);
        hotelService.setUser(1, 5000);

        // When/Then
        assertThrows(InvalidDateRangeException.class, () ->
                hotelService.bookRoom(1, 2, date(2026, 7, 7), date(2026, 6, 30)));
    }

    @Test
    @DisplayName("bookRoom should FAIL when room is already booked for that period")
    void bookRoom_Fails_WhenRoomNotAvailable() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 5000);
        hotelService.setUser(2, 5000);
        hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8));

        // When/Then
        assertThrows(RoomNotAvailableException.class, () ->
                hotelService.bookRoom(2, 1, date(2026, 7, 7), date(2026, 7, 9)));
    }

    @Test
    @DisplayName("bookRoom should FAIL when room does not exist")
    void bookRoom_Fails_WhenRoomNotFound() {
        // Given
        hotelService.setUser(1, 5000);

        // When/Then
        assertThrows(RoomNotFoundException.class, () ->
                hotelService.bookRoom(1, 99, date(2026, 7, 7), date(2026, 7, 8)));
    }

    @Test
    @DisplayName("bookRoom should FAIL when user does not exist")
    void bookRoom_Fails_WhenUserNotFound() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);

        // When/Then
        assertThrows(UserNotFoundException.class, () ->
                hotelService.bookRoom(99, 1, date(2026, 7, 7), date(2026, 7, 8)));
    }

    @Test
    @DisplayName("bookRoom should SUCCEED and deduct balance correctly")
    void bookRoom_Succeeds_AndDeductsBalance() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 5000);

        // When
        assertDoesNotThrow(() ->
                hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8)));

        // Then
        User user = store.findUserById(1).orElse(null);
        assertEquals(4000, user.getBalance());
    }

    @Test
    @DisplayName("bookRoom should calculate nights correctly")
    void bookRoom_CalculatesNightsCorrectly() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 20000);

        // Test 1: 7 nights
        hotelService.bookRoom(1, 1, date(2026, 6, 30), date(2026, 7, 7));
        Booking booking1 = store.getAllBookings().get(0);
        assertEquals(7, booking1.getNumberOfNights());
        assertEquals(7000, booking1.getTotalCost());

        // Reset
        store.getAllBookings().clear();
        hotelService.setUser(1, 20000);

        // Test 2: 1 night
        hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8));
        Booking booking2 = store.getAllBookings().get(0);
        assertEquals(1, booking2.getNumberOfNights());
        assertEquals(1000, booking2.getTotalCost());
    }

    // =====================================================
    // TEST GROUP 4: OVERLAP DETECTION (EDGE CASES)
    // =====================================================

    @Test
    @DisplayName("Booking should FAIL when new booking starts during existing booking")
    void bookRoom_Fails_WhenStartsDuringExistingBooking() {
        // Given: Existing booking 05/07 to 10/07
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 10000);
        hotelService.setUser(2, 10000);
        hotelService.bookRoom(1, 1, date(2026, 7, 5), date(2026, 7, 10));

        // When/Then: New booking 07/07 to 12/07 should FAIL
        assertThrows(RoomNotAvailableException.class, () ->
                hotelService.bookRoom(2, 1, date(2026, 7, 7), date(2026, 7, 12)));
    }

    @Test
    @DisplayName("Booking should FAIL when new booking ends during existing booking")
    void bookRoom_Fails_WhenEndsDuringExistingBooking() {
        // Given: Existing booking 05/07 to 10/07
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 10000);
        hotelService.setUser(2, 10000);
        hotelService.bookRoom(1, 1, date(2026, 7, 5), date(2026, 7, 10));

        // When/Then: New booking 01/07 to 07/07 should FAIL
        assertThrows(RoomNotAvailableException.class, () ->
                hotelService.bookRoom(2, 1, date(2026, 7, 1), date(2026, 7, 7)));
    }

    @Test
    @DisplayName("Booking should FAIL when new booking contains existing booking")
    void bookRoom_Fails_WhenContainsExistingBooking() {
        // Given: Existing booking 05/07 to 10/07
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 20000);
        hotelService.setUser(2, 20000);
        hotelService.bookRoom(1, 1, date(2026, 7, 5), date(2026, 7, 10));

        // When/Then: New booking 01/07 to 15/07 should FAIL
        assertThrows(RoomNotAvailableException.class, () ->
                hotelService.bookRoom(2, 1, date(2026, 7, 1), date(2026, 7, 15)));
    }

    @Test
    @DisplayName("Booking should FAIL when new booking is inside existing booking")
    void bookRoom_Fails_WhenInsideExistingBooking() {
        // Given: Existing booking 01/07 to 15/07
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 20000);
        hotelService.setUser(2, 20000);
        hotelService.bookRoom(1, 1, date(2026, 7, 1), date(2026, 7, 15));

        // When/Then: New booking 05/07 to 10/07 should FAIL
        assertThrows(RoomNotAvailableException.class, () ->
                hotelService.bookRoom(2, 1, date(2026, 7, 5), date(2026, 7, 10)));
    }

    @Test
    @DisplayName("Booking should SUCCEED when dates are adjacent (no overlap)")
    void bookRoom_Succeeds_WhenDatesAreAdjacent() {
        // Given: Existing booking 05/07 to 10/07
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 10000);
        hotelService.setUser(2, 10000);
        hotelService.bookRoom(1, 1, date(2026, 7, 5), date(2026, 7, 10));

        // When/Then: New booking 10/07 to 15/07 should SUCCEED (checkout = checkin is OK)
        assertDoesNotThrow(() ->
                hotelService.bookRoom(2, 1, date(2026, 7, 10), date(2026, 7, 15)));
    }

    @Test
    @DisplayName("Booking should SUCCEED when dates don't overlap at all")
    void bookRoom_Succeeds_WhenNoOverlap() {
        // Given: Existing booking 05/07 to 10/07
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 10000);
        hotelService.setUser(2, 10000);
        hotelService.bookRoom(1, 1, date(2026, 7, 5), date(2026, 7, 10));

        // When/Then: New booking 15/07 to 20/07 should SUCCEED
        assertDoesNotThrow(() ->
                hotelService.bookRoom(2, 1, date(2026, 7, 15), date(2026, 7, 20)));
    }

    // =====================================================
    // TEST GROUP 5: PRINT FUNCTIONS
    // =====================================================

    @Test
    @DisplayName("printAll should display rooms from newest to oldest")
    void printAll_DisplaysRoomsNewestToOldest() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setRoom(2, RoomType.JUNIOR_SUITE, 2000);
        hotelService.setRoom(3, RoomType.MASTER_SUITE, 3000);

        // Then - verify order in ArrayList
        assertEquals(3, store.getAllRooms().size());
        // Rooms are stored in insertion order, printAll() reverses them
        assertEquals(1, store.getAllRooms().get(0).getRoomNumber());
        assertEquals(2, store.getAllRooms().get(1).getRoomNumber());
        assertEquals(3, store.getAllRooms().get(2).getRoomNumber());
    }

    @Test
    @DisplayName("printAll should display bookings from newest to oldest")
    void printAll_DisplaysBookingsNewestToOldest() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setRoom(2, RoomType.JUNIOR_SUITE, 2000);
        hotelService.setUser(1, 10000);
        hotelService.setUser(2, 10000);
        hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8));
        hotelService.bookRoom(2, 2, date(2026, 7, 7), date(2026, 7, 8));

        // Then - verify order in ArrayList
        assertEquals(2, store.getAllBookings().size());
        // Bookings stored in insertion order, printAll() reverses them
        assertEquals(1, store.getAllBookings().get(0).getBookingId());
        assertEquals(2, store.getAllBookings().get(1).getBookingId());
    }

    @Test
    @DisplayName("printAll bookings should show SNAPSHOT data, not current data")
    void printAll_ShowsSnapshotData() {
        // Given
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setUser(1, 10000);
        hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8));

        // Get booking
        Booking booking = store.getAllBookings().get(0);
        assertEquals(1000, booking.getRoomSnapshot().getPricePerNight());

        // When - change room price
        hotelService.setRoom(1, RoomType.MASTER_SUITE, 5000);

        // Then - booking should still show original price
        assertEquals(1000, booking.getRoomSnapshot().getPricePerNight());
        assertEquals(RoomType.STANDARD, booking.getRoomSnapshot().getRoomType());
    }

    @Test
    @DisplayName("printAllUsers should display users from newest to oldest")
    void printAllUsers_DisplaysUsersNewestToOldest() {
        // Given
        hotelService.setUser(1, 5000);
        hotelService.setUser(2, 10000);

        // Then - verify order in ArrayList
        assertEquals(2, store.getAllUsers().size());
        // Users stored in insertion order, printAllUsers() reverses them
        assertEquals(1, store.getAllUsers().get(0).getUserId());
        assertEquals(2, store.getAllUsers().get(1).getUserId());
    }

    // =====================================================
    // TEST GROUP 6: PDF TEST CASE (INTEGRATION)
    // =====================================================

    @Test
    @DisplayName("Full PDF Test Case Scenario")
    void fullPdfTestCase() {
        // 1. Create rooms
        hotelService.setRoom(1, RoomType.STANDARD, 1000);
        hotelService.setRoom(2, RoomType.JUNIOR_SUITE, 2000);
        hotelService.setRoom(3, RoomType.MASTER_SUITE, 3000);

        // 2. Create users
        hotelService.setUser(1, 5000);
        hotelService.setUser(2, 10000);

        // 3. User 1 books Room 2: 30/06 to 07/07 (7 nights = 14000)
        //    Expected: FAIL (5000 < 14000)
        assertThrows(InsufficientBalanceException.class, () ->
                hotelService.bookRoom(1, 2, date(2026, 6, 30), date(2026, 7, 7)));
        // Verify balance unchanged
        assertEquals(5000, getUserBalance(1));

        // 4. User 1 books Room 2: 07/07 to 30/06 (invalid dates)
        //    Expected: FAIL
        assertThrows(InvalidDateRangeException.class, () ->
                hotelService.bookRoom(1, 2, date(2026, 7, 7), date(2026, 6, 30)));

        // 5. User 1 books Room 1: 07/07 to 08/07 (1 night = 1000)
        //    Expected: SUCCESS, balance = 4000
        assertDoesNotThrow(() ->
                hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8)));
        assertEquals(4000, getUserBalance(1));

        // 6. User 2 books Room 1: 07/07 to 09/07 (overlaps with booking 5)
        //    Expected: FAIL
        assertThrows(RoomNotAvailableException.class, () ->
                hotelService.bookRoom(2, 1, date(2026, 7, 7), date(2026, 7, 9)));
        // Verify balance unchanged
        assertEquals(10000, getUserBalance(2));

        // 7. User 2 books Room 3: 07/07 to 08/07 (1 night = 3000)
        //    Expected: SUCCESS, balance = 7000
        assertDoesNotThrow(() ->
                hotelService.bookRoom(2, 3, date(2026, 7, 7), date(2026, 7, 8)));
        assertEquals(7000, getUserBalance(2));

        // 8. Update Room 1 to MASTER_SUITE, 10000
        hotelService.setRoom(1, RoomType.MASTER_SUITE, 10000);

        // 9. Verify final state
        // - Room 1 should now be MASTER_SUITE, 10000
        Room room1 = store.findRoomByNumber(1).orElse(null);
        assertNotNull(room1);
        assertEquals(RoomType.MASTER_SUITE, room1.getRoomType());
        assertEquals(10000, room1.getPricePerNight());

        // - But booking for Room 1 should still show STANDARD, 1000
        Booking booking1 = store.getAllBookings().get(0);
        assertEquals(RoomType.STANDARD, booking1.getRoomSnapshot().getRoomType());
        assertEquals(1000, booking1.getRoomSnapshot().getPricePerNight());

        // 10. Print results (for visual verification)
        System.out.println("\n=== FULL PDF TEST CASE OUTPUT ===");
        hotelService.printAll();
        hotelService.printAllUsers();
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private int getUserBalance(int userId) {
        return store.findUserById(userId)
                .map(User::getBalance)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }
}