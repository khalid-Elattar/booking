package com.skypay.booking.application;

import com.skypay.booking.domain.model.RoomType;
import com.skypay.booking.domain.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

/**
 * CommandLineRunner to execute test cases for the Hotel Reservation System.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HotelApplicationRunner implements CommandLineRunner {

    private final HotelService hotelService;

    @Override
    public void run(String... args) {
        log.info("=== Starting Hotel Reservation System Test Cases ===\n");

        try {
            // 1. Create 3 rooms
            log.info("--- Step 1: Creating 3 rooms ---");
            hotelService.setRoom(1, RoomType.STANDARD, 1000);
            hotelService.setRoom(2, RoomType.JUNIOR_SUITE, 2000);
            hotelService.setRoom(3, RoomType.MASTER_SUITE, 3000);

            // 2. Create 2 users
            log.info("\n--- Step 2: Creating 2 users ---");
            hotelService.setUser(1, 5000);
            hotelService.setUser(2, 10000);

            // 3. User 1 books Room 2: 30/06/2026 to 07/07/2026 (7 nights = 14000)
            // Should FAIL - insufficient balance (5000 < 14000)
            log.info("\n--- Step 3: User 1 attempts to book Room 2 (30 Jun - 07 Jul 2026) - Should FAIL (insufficient balance) ---");
            try {
                hotelService.bookRoom(1, 2, date(2026, 6, 30), date(2026, 7, 7));
            } catch (Exception e) {
                // Expected to fail
            }

            // 4. User 1 books Room 2: 07/07/2026 to 30/06/2026
            // Should FAIL - invalid date range (checkOut before checkIn)
            log.info("\n--- Step 4: User 1 attempts to book Room 2 (reversed dates) - Should FAIL (invalid date range) ---");
            try {
                hotelService.bookRoom(1, 2, date(2026, 7, 7), date(2026, 6, 30));
            } catch (Exception e) {
                // Expected to fail
            }

            // 5. User 1 books Room 1: 07/07/2026 to 08/07/2026 (1 night = 1000)
            // Should SUCCEED - balance 5000 >= 1000, room is free
            // User 1 new balance = 4000
            log.info("\n--- Step 5: User 1 books Room 1 (07-08 Jul 2026) - Should SUCCEED ---");
            hotelService.bookRoom(1, 1, date(2026, 7, 7), date(2026, 7, 8));

            // 6. User 2 books Room 1: 07/07/2026 to 09/07/2026 (2 nights)
            // Should FAIL - room already booked for overlapping period
            log.info("\n--- Step 6: User 2 attempts to book Room 1 (07-09 Jul 2026) - Should FAIL (room not available) ---");
            try {
                hotelService.bookRoom(2, 1, date(2026, 7, 7), date(2026, 7, 9));
            } catch (Exception e) {
                // Expected to fail
            }

            // 7. User 2 books Room 3: 07/07/2026 to 08/07/2026 (1 night = 3000)
            // Should SUCCEED - balance 10000 >= 3000, room is free
            // User 2 new balance = 7000
            log.info("\n--- Step 7: User 2 books Room 3 (07-08 Jul 2026) - Should SUCCEED ---");
            hotelService.bookRoom(2, 3, date(2026, 7, 7), date(2026, 7, 8));

            // 8. Update Room 1 to MASTER_SUITE with price 10000
            // Previous booking for Room 1 should still show STANDARD at 1000
            log.info("\n--- Step 8: Updating Room 1 (STANDARD -> MASTER_SUITE, 1000 -> 10000) ---");
            hotelService.setRoom(1, RoomType.MASTER_SUITE, 10000);

            // 9. Print results
            log.info("\n--- Step 9: Printing all results ---");
            hotelService.printAll();
            hotelService.printAllUsers();

            log.info("\n=== Test Cases Completed Successfully ===");

        } catch (Exception e) {
            log.error("Unexpected error during test execution", e);
        }
    }

    /**
     * Helper method to create a Date object from year, month (1-12), and day.
     *
     * @param year the year
     * @param month the month (1-12)
     * @param day the day
     * @return Date object
     */
    private Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}