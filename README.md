# Hotel Reservation System

A simplified Hotel Reservation System built with Java and Spring Boot 3.4.12, following Domain-Driven Design (DDD) principles with Lombok for boilerplate reduction.

## Tech Stack

- **Java**: 21
- **Spring Boot**: 3.4.12
- **Lombok**: For reducing boilerplate code
- **Maven**: Build tool
- **JUnit 5**: Testing framework

## Project Structure

The project follows DDD principles with clear separation of concerns:

```
src/main/java/com/skypay/booking/
├── BookingApplication.java                    # Main Spring Boot application
├── domain/
│   ├── model/
│   │   ├── User.java                         # User entity
│   │   ├── Room.java                         # Room entity
│   │   ├── Booking.java                      # Booking entity
│   │   ├── RoomType.java                     # Room type enumeration
│   │   ├── RoomSnapshot.java                 # Immutable room state snapshot
│   │   └── UserSnapshot.java                 # Immutable user state snapshot
│   ├── exception/
│   │   ├── InsufficientBalanceException.java
│   │   ├── RoomNotAvailableException.java
│   │   ├── RoomNotFoundException.java
│   │   ├── UserNotFoundException.java
│   │   └── InvalidDateRangeException.java
│   └── service/
│       └── HotelService.java                  # Core business logic
├── application/
│   └── HotelApplicationRunner.java            # CommandLineRunner with test cases
└── infrastructure/
    └── repository/
        └── InMemoryStore.java                 # In-memory data storage
```

## Key Features

### 1. Snapshot Pattern for Bookings
- **Critical Design**: Bookings store immutable snapshots of room and user data at the time of booking
- When a room's price is updated via `setRoom()`, existing bookings retain the original price
- Ensures historical accuracy and prevents retroactive changes

### 2. Comprehensive Validation
- **Balance Validation**: Ensures users have sufficient funds before booking
- **Date Validation**: Checks that check-in is before check-out
- **Availability Validation**: Prevents double-booking with robust overlap detection

### 3. Domain-Driven Design
- Clear separation between domain, application, and infrastructure layers
- Rich domain models with business logic encapsulated in entities
- Custom exceptions for domain-specific error handling

## How to Run

### Prerequisites
- Java 17 or higher (Java 21 recommended)
- Maven 3.6 or higher

### Steps

1. **Clone or navigate to the project directory**
   ```bash
   cd /path/to/booking
   ```

2. **Build the project**
   ```bash
   ./mvnw clean install
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Alternative: Run the JAR directly**
   ```bash
   ./mvnw clean package
   java -jar target/booking-0.0.1-SNAPSHOT.jar
   ```

### Expected Output

The application will execute test cases demonstrating:
- Creating rooms and users
- Successful bookings
- Failed bookings (insufficient balance, invalid dates, room unavailability)
- Room updates that don't affect existing bookings
- Printing all rooms, bookings, and users

## Design Questions & Answers

### 1. Is putting all functions in the same service the recommended approach?

**Short Answer**: No, it violates the Single Responsibility Principle.

**Detailed Analysis**:

#### Current Issues:
- **Violates SRP**: `HotelService` handles room management, user management, and booking operations - three distinct responsibilities
- **Low Cohesion**: Methods for rooms, users, and bookings are mixed together
- **Difficult to Test**: Testing booking logic requires understanding of room and user management
- **Poor Scalability**: As requirements grow, the service becomes a "god object"

#### Recommended Alternatives:

**Option 1: Separate Domain Services**
```java
@Service
public class RoomService {
    void createRoom(...);
    void updateRoom(...);
    Room findRoom(...);
}

@Service
public class UserService {
    void createUser(...);
    void updateUser(...);
    User findUser(...);
}

@Service
public class BookingService {
    private final RoomService roomService;
    private final UserService userService;

    void bookRoom(...) {
        // Coordinates between RoomService and UserService
    }
}
```

**Benefits**:
- Each service has a single, well-defined responsibility
- Easier to test in isolation
- Better code organization and maintainability
- Follows Domain-Driven Design principles

**Option 2: Application Service + Domain Services**
```java
// Domain Services (pure business logic)
@Service
public class RoomDomainService { /* room-specific logic */ }

@Service
public class UserDomainService { /* user-specific logic */ }

@Service
public class BookingDomainService { /* booking-specific logic */ }

// Application Service (orchestration)
@Service
public class HotelApplicationService {
    private final RoomDomainService roomService;
    private final UserDomainService userService;
    private final BookingDomainService bookingService;

    // Coordinates workflow across domain services
}
```

**Benefits**:
- Clear separation between orchestration and business logic
- Domain services remain pure and testable
- Application service handles cross-cutting concerns

### 2. What are alternatives to the snapshot pattern for preventing setRoom() from impacting previous bookings?

**Current Implementation**: Snapshot Pattern
- Creates immutable copies of room/user state at booking time
- Simple and effective for small-scale applications

#### Alternative 1: Event Sourcing

**Concept**: Store all changes as a sequence of events instead of current state.

```java
// Events
class RoomCreatedEvent { int roomNumber; RoomType type; int price; LocalDateTime timestamp; }
class RoomPriceChangedEvent { int roomNumber; int oldPrice; int newPrice; LocalDateTime timestamp; }
class BookingCreatedEvent { int bookingId; int roomNumber; LocalDate checkIn; ... }

// To get room state at booking time, replay events up to that timestamp
Room getRoomStateAt(int roomNumber, LocalDateTime timestamp) {
    return eventStore.getEvents(roomNumber)
        .filter(e -> e.timestamp.isBefore(timestamp))
        .reduce(new Room(), (room, event) -> event.apply(room));
}
```

**Pros**:
- Complete audit trail of all changes
- Can reconstruct state at any point in time
- Supports complex business analytics
- Natural fit for event-driven architectures

**Cons**:
- Significantly more complex to implement
- Higher storage requirements
- Query performance can be slower
- Requires event versioning strategy

**Use Cases**: Banking, financial systems, compliance-heavy domains

#### Alternative 2: Temporal/Versioning Tables

**Concept**: Maintain version history of entities with effective date ranges.

```java
@Entity
class RoomVersion {
    int roomNumber;
    RoomType roomType;
    int pricePerNight;
    LocalDateTime effectiveFrom;
    LocalDateTime effectiveTo;  // null for current version
}

@Entity
class Booking {
    int bookingId;
    int roomNumber;
    int roomVersionId;  // References specific version
    LocalDate checkIn;
    LocalDate checkOut;
}
```

**Pros**:
- Simpler than event sourcing
- Easy to query historical data
- Native database support (SQL:2011 temporal tables)
- Good balance of complexity and features

**Cons**:
- More storage than snapshot pattern
- Slightly more complex queries
- Requires maintenance of version history

**Use Cases**: Pricing systems, product catalogs, insurance

#### Alternative 3: Immutable Entities

**Concept**: Never update entities; create new versions instead.

```java
@Entity
class Room {
    UUID id;  // New UUID for each version
    int roomNumber;
    RoomType type;
    int price;
    boolean isLatest;
}

// When updating room, create new record and mark old as !isLatest
```

**Pros**:
- Simple conceptual model
- Natural audit trail
- Works well with distributed systems

**Cons**:
- Data duplication
- Need to handle "latest version" queries carefully
- Foreign key relationships become complex

#### Alternative 4: Copy-on-Write References

**Concept**: Bookings maintain deep copies of referenced data.

```java
@Entity
class Booking {
    @Embedded  // Not a reference, but a copy
    RoomData roomData;

    @Embedded
    UserData userData;
}
```

**Pros**:
- No additional tables needed
- Simple to implement
- Fast reads (no joins)

**Cons**:
- Data duplication (similar to snapshots)
- Schema changes require migration
- Updates don't propagate (which is intentional here)

### Recommendation

**For this Hotel Reservation System**: **Snapshot Pattern (Current Implementation)**

**Justification**:
1. **Simplicity**: The snapshot pattern is the simplest solution that meets the requirements
2. **Performance**: No complex event replay or temporal queries needed
3. **Scale**: For a hotel system with moderate booking volume, storage overhead is minimal
4. **Requirements**: The spec explicitly states bookings should preserve original prices - snapshots do this perfectly
5. **Maintainability**: Easy for other developers to understand and maintain

**When to Consider Alternatives**:
- **Event Sourcing**: If you need complete audit trails, regulatory compliance, or complex analytics
- **Versioning**: If you need to query historical pricing across many entities or support "what-if" scenarios
- **Immutable Entities**: If you're building a microservices architecture with eventual consistency

**Hybrid Approach for Production**:
For a production system, I'd recommend:
1. Keep snapshot pattern for bookings (current implementation)
2. Add event logging for audit trail (without full event sourcing)
3. Add soft deletes and `updatedAt` timestamps for compliance
4. Consider read models (CQRS) if query performance becomes an issue

```java
@Entity
class Booking {
    RoomSnapshot roomSnapshot;  // Keep this
    UserSnapshot userSnapshot;  // Keep this

    @Embedded
    AuditInfo auditInfo;  // Add this for compliance
}

@Embeddable
class AuditInfo {
    LocalDateTime createdAt;
    String createdBy;
    LocalDateTime updatedAt;
    String updatedBy;
}
```

This provides the simplicity of snapshots with the traceability needed for production systems.

## Testing

The application includes a comprehensive test suite executed via `HotelApplicationRunner`:

1. ✓ Create rooms with different types and prices
2. ✓ Create users with balances
3. ✗ Fail booking due to insufficient balance
4. ✗ Fail booking due to invalid date range
5. ✓ Successful booking with balance deduction
6. ✗ Fail booking due to room unavailability
7. ✓ Successful booking for different room
8. ✓ Update room without affecting existing bookings
9. ✓ Print all data showing snapshot preservation

## License

This project is a technical test implementation for educational purposes.