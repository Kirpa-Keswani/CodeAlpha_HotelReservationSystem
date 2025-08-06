import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Room Category Enum
enum RoomCategory {
    STANDARD, DELUXE, SUITE;
}

// Room Class
class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roomNumber;
    private RoomCategory category;

    public Room(int roomNumber, RoomCategory category) {
        this.roomNumber = roomNumber;
        this.category = category;
    }

    public int getRoomNumber() { return roomNumber; }
    public RoomCategory getCategory() { return category; }

    @Override
    public String toString() {
        return "Room " + roomNumber + " (" + category + ")";
    }
}

// Reservation Class
class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reservationId;
    private String guestName;
    private Room room;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private boolean paid;

    public Reservation(String reservationId, String guestName, Room room,
                       LocalDate checkInDate, LocalDate checkOutDate) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.paid = false;
    }

    // Getters and setters
    public String getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public Room getRoom() { return room; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return "Reservation " + reservationId + " for " + guestName + ", " +
               room + ", from " + checkInDate.format(fmt) + " to " + checkOutDate.format(fmt) +
               (paid ? " [PAID]" : " [NOT PAID]");
    }
}

// HotelReservationSystem Class: main logic for managing rooms and reservations
public class HotelReservationSystem {

    private List<Room> rooms;
    private Map<String, Reservation> reservations; // reservationId -> Reservation
    private Scanner scanner;

    private static final String RESERVATIONS_FILE = "reservations.dat";
    private static final String ROOMS_FILE = "rooms.dat";

    public HotelReservationSystem() {
        scanner = new Scanner(System.in);
        rooms = new ArrayList<>();
        reservations = new HashMap<>();
        loadRooms();
        loadReservations();
    }

    // Initialize default rooms if none exist
    private void initializeRooms() {
        if (!rooms.isEmpty()) return;

        int roomNum = 101;
        // Add standard rooms 101-105
        for (int i = 0; i < 5; i++) {
            rooms.add(new Room(roomNum++, RoomCategory.STANDARD));
        }
        // Add deluxe rooms 201-204
        roomNum = 201;
        for (int i = 0; i < 4; i++) {
            rooms.add(new Room(roomNum++, RoomCategory.DELUXE));
        }
        // Add suite rooms 301-302
        roomNum = 301;
        for (int i = 0; i < 2; i++) {
            rooms.add(new Room(roomNum++, RoomCategory.SUITE));
        }
    }

    // Save rooms to file (just once)
    private void saveRooms() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ROOMS_FILE))) {
            oos.writeObject(rooms);
        } catch (IOException e) {
            System.err.println("Error saving rooms: " + e.getMessage());
        }
    }

    // Load rooms from file
    @SuppressWarnings("unchecked")
    private void loadRooms() {
        File file = new File(ROOMS_FILE);
        if (!file.exists()) {
            initializeRooms();
            saveRooms();
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ROOMS_FILE))) {
            rooms = (List<Room>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Error loading rooms, initializing defaults: " + e.getMessage());
            initializeRooms();
            saveRooms();
        }
    }

    // Save reservations to file
    private void saveReservations() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RESERVATIONS_FILE))) {
            oos.writeObject(reservations);
        } catch (IOException e) {
            System.err.println("Error saving reservations: " + e.getMessage());
        }
    }

    // Load reservations from file
    @SuppressWarnings("unchecked")
    private void loadReservations() {
        File file = new File(RESERVATIONS_FILE);
        if (!file.exists()) {
            reservations = new HashMap<>();
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(RESERVATIONS_FILE))) {
            reservations = (Map<String, Reservation>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Error loading reservations, starting empty: " + e.getMessage());
            reservations = new HashMap<>();
        }
    }

    // Search available rooms within date range and category
    private List<Room> searchAvailableRooms(RoomCategory category, LocalDate checkIn, LocalDate checkOut) {
        List<Room> availableRooms = new ArrayList<>();
        outer: for (Room room : rooms) {
            if (room.getCategory() != category) continue;

            // Check if this room is booked in the given range
            for (Reservation res : reservations.values()) {
                if (res.getRoom().getRoomNumber() == room.getRoomNumber()) {
                    // If date ranges overlap -> not available
                    if (!(checkOut.isBefore(res.getCheckInDate()) || checkIn.isAfter(res.getCheckOutDate().minusDays(1)))) {
                        continue outer;
                    }
                }
            }
            availableRooms.add(room);
        }
        return availableRooms;
    }

    // Make reservation
    private void makeReservation() {
        System.out.println("Enter your name:");
        String guestName = scanner.nextLine().trim();

        System.out.println("Choose room category:");
        for (RoomCategory rc : RoomCategory.values()) {
            System.out.println(rc.ordinal() + 1 + ". " + rc);
        }
        int categoryChoice = getIntInput(1, RoomCategory.values().length);
        RoomCategory category = RoomCategory.values()[categoryChoice - 1];

        System.out.println("Enter check-in date (yyyy-MM-dd):");
        LocalDate checkIn = getDateInput();

        System.out.println("Enter check-out date (yyyy-MM-dd):");
        LocalDate checkOut = getDateInput();

        // Validate date range
        if (!checkOut.isAfter(checkIn)) {
            System.out.println("Check-out date must be after check-in date.");
            return;
        }

        List<Room> availableRooms = searchAvailableRooms(category, checkIn, checkOut);
        if (availableRooms.isEmpty()) {
            System.out.println("No available rooms of that category for given dates.");
            return;
        }

        System.out.println("Available rooms:");
        for (int i = 0; i < availableRooms.size(); i++) {
            System.out.println((i + 1) + ". " + availableRooms.get(i));
        }

        System.out.println("Select room to book (enter number):");
        int roomChoice = getIntInput(1, availableRooms.size());
        Room selectedRoom = availableRooms.get(roomChoice - 1);

        // Generate simple reservation id
        String reservationId = "RES" + System.currentTimeMillis();

        Reservation reservation = new Reservation(reservationId, guestName, selectedRoom, checkIn, checkOut);

        // Simulate payment
        if (!processPayment(selectedRoom, checkIn, checkOut)) {
            System.out.println("Payment failed. Reservation not completed.");
            return;
        }
        reservation.setPaid(true);

        reservations.put(reservationId, reservation);
        saveReservations();

        System.out.println("Reservation successful! Your reservation ID is " + reservationId);
    }

    // Cancel reservation
    private void cancelReservation() {
        System.out.println("Enter your reservation ID:");
        String resId = scanner.nextLine().trim();

        Reservation res = reservations.get(resId);
        if (res == null) {
            System.out.println("Reservation ID not found.");
            return;
        }

        System.out.println("Are you sure you want to cancel your reservation? (y/n)");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("y")) {
            reservations.remove(resId);
            saveReservations();
            System.out.println("Reservation cancelled successfully.");
        } else {
            System.out.println("Cancellation aborted.");
        }
    }

    // View all reservations
    private void viewReservations() {
        if (reservations.isEmpty()) {
            System.out.println("No reservations found.");
            return;
        }

        System.out.println("All Reservations:");
        for (Reservation res : reservations.values()) {
            System.out.println(res);
        }
    }

    // Simulate payment processing (returns true if success)
    private boolean processPayment(Room room, LocalDate checkIn, LocalDate checkOut) {
        // Compute cost based on category and nights
        long nights = checkIn.until(checkOut).getDays();
        double ratePerNight = 0;
        switch (room.getCategory()) {
            case STANDARD: ratePerNight = 100; break;
            case DELUXE: ratePerNight = 150; break;
            case SUITE: ratePerNight = 250; break;
        }
        double totalCost = nights * ratePerNight;
        System.out.printf("Total cost for %d night(s) in %s: $%.2f%n", nights, room.getCategory(), totalCost);

        System.out.println("Proceed with payment? (y/n)");
        String response = scanner.nextLine().trim().toLowerCase();
        if (!response.equals("y")) {
            return false;
        }

        // Simple simulated payment (always success here)
        System.out.println("Payment successful.");
        return true;
    }

    // Utility: parse user integer input within bounds
    private int getIntInput(int min, int max) {
        while (true) {
            System.out.print("Enter choice (" + min + "-" + max + "): ");
            String input = scanner.nextLine();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) return val;
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid input. Try again.");
        }
    }

    // Utility: parse user date input yyyy-MM-dd
    private LocalDate getDateInput() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            try {
                String dateStr = scanner.nextLine().trim();
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date;
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            }
        }
    }

    // Main menu
    private void showMenu() {
        System.out.println("\n--- Hotel Reservation System ---");
        System.out.println("1. Search and Book Room");
        System.out.println("2. Cancel Reservation");
        System.out.println("3. View All Reservations");
        System.out.println("4. Exit");
    }

    // Run the system loop
    public void run() {
        while (true) {
            showMenu();
            int choice = getIntInput(1, 4);

            switch (choice) {
                case 1 -> makeReservation();
                case 2 -> cancelReservation();
                case 3 -> viewReservations();
                case 4 -> {
                    System.out.println("Exiting... Goodbye!");
                    scanner.close();
                    return;
                }
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        HotelReservationSystem system = new HotelReservationSystem();
        system.run();
    }
}
