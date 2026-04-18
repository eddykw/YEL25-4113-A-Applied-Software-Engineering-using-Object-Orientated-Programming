import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JourneyManager {

    private static final String JOURNEYS_FILE = "journeys.csv";
    private static final String PROFILE_FILE = "profile.json";
    private static final String CONFIG_FILE = "config.json";
    private static final String REPORTS_FOLDER = "reports";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final Scanner scanner;
    private ArrayList<Journey> journeys;

    private String activeName;
    private Journey.PassengerType activePassengerType;
    private String activeDefaultPayment;
    private int nextId;
    private Journey lastDeletedJourney;

    private HashMap<String, BigDecimal> baseFares;
    private HashMap<Journey.PassengerType, BigDecimal> discountRates;
    private HashMap<Journey.PassengerType, BigDecimal> dailyCaps;
    private String peakStart;
    private String peakEnd;

    public JourneyManager() {
        scanner = new Scanner(System.in);
        journeys = new ArrayList<>();
        activeName = "";
        activePassengerType = null;
        activeDefaultPayment = "";
        nextId = 1;
        lastDeletedJourney = null;

        setDefaultConfig();
        loadConfig();
        loadProfile();
        loadJourneys();
        rebuildAllCharges();
    }

    // Starts the program loop

    public void start() {
        boolean running = true;

        while (running) {
            showMainMenu();
            int choice = readInt("Enter your choice: ");

            if (choice == 1) {
                riderMenu();
            } else if (choice == 2) {
                adminMenu();
            } else if (choice == 3) {
                exitProgram();
                running = false;
            } else {
                System.out.println("Invalid choice. Try again!");
            }
        }

        scanner.close();
    }
    private void showMainMenu() {
        System.out.println("\n--- CityRide Lite---");
        System.out.println("1. Rider Menu");
        System.out.println("2. Admin Menu");
        System.out.println("3. Exit");
    }

    private void riderMenu() {
        boolean inRiderMenu = true;

        while (inRiderMenu) {
            showRiderMenu();
            int choice = readInt("Enter your choice: ");

            if (choice == 1) {
                createOrChangeProfile();
            } else if (choice == 2) {
                loadProfile();
                viewActiveProfile();
            } else if (choice == 3) {
                saveProfile();
                System.out.println("Profile saved!");
            } else if (choice == 4) {
                viewActiveProfile();
            } else if (choice == 5) {
                addJourney();
            } else if (choice == 6) {
                editJourney();
            } else if (choice == 7) {
                removeJourney();
            } else if (choice == 8) {
                undoDelete();
            } else if (choice == 9) {
                displayJourneys();
            } else if (choice == 10) {
                viewRunningTotals();
            } else if (choice == 11) {
                importJourneysFromCsv();
            } else if (choice == 12) {
                exportJourneysBackup();
            } else if (choice == 13) {
                showDailySummary();
            } else if (choice == 14) {
                exportSummaryCsvReport();
            } else if (choice == 15) {
                exportSummaryTextReport();
            } else if (choice == 16) {
                saveCurrentState();
            } else if (choice == 17) {
                inRiderMenu = false;
            } else {
                System.out.println("Invalid choice. Try again!");
            }
        }
    }

    private void showRiderMenu() {
        System.out.println("\n--- Rider Menu ---");
        System.out.println("1.  Create/Change Profile");
        System.out.println("2.  Load Profile from JSON");
        System.out.println("3.  Save Profile to JSON");
        System.out.println("4.  View Active Profile");
        System.out.println("5.  Add Journey");
        System.out.println("6.  Edit Journey");
        System.out.println("7.  Delete Journey");
        System.out.println("8.  Undo Last Delete");
        System.out.println("9.  View All Journeys");
        System.out.println("10. View Running Totals/ Cap Status");
        System.out.println("11. Import Journeys from CSV");
        System.out.println("12. Export Current Journeys to CSV");
        System.out.println("13. Daily Summary");
        System.out.println("14. Export Summary Report as CSV");
        System.out.println("15. Export Summary Report as TXT");
        System.out.println("16. Save Current Day State");
        System.out.println("17. Back");
    }

    private void adminMenu() {
        System.out.print("Enter admin password: ");
        String password = scanner.nextLine().trim();

        if (!ADMIN_PASSWORD.equals(password)) {
            System.out.println("Incorrect password. Access denied!");
            return;
        }

        boolean inAdminMenu = true;

        while (inAdminMenu) {
            showAdminMenu();
            int choice = readInt("Enter your choice: ");

            if (choice == 1) {
                viewConfig();
            } else if (choice == 2) {
                updateDiscount();
            } else if (choice == 3) {
                deleteDiscount();
            } else if (choice == 4) {
                updateDailyCap();
            } else if (choice == 5) {
                deleteDailyCap();
            } else if (choice == 6) {
                updateBaseFare();
            } else if (choice == 7) {
                deleteBaseFare();
            } else if (choice == 8) {
                updatePeakWindow();
            } else if (choice == 9) {
                saveConfig();
                System.out.println("Configuration saved!");
            } else if (choice == 10) {
                inAdminMenu = false;
            } else {
                System.out.println("Invalid choice. Try again!");
            }
        }
    }

    private void showAdminMenu() {
        System.out.println("\n--- Admin Menu ---");
        System.out.println("1.  View Active Config");
        System.out.println("2.  Update Discount Rate");
        System.out.println("3.  Delete Discount Rate (reset to 0.00)");
        System.out.println("4.  Update Daily Cap");
        System.out.println("5.  Delete Daily Cap");
        System.out.println("6.  Update Base Fare");
        System.out.println("7.  Delete Base Fare");
        System.out.println("8.  Update Peak Window");
        System.out.println("9.  Save Config to JSON");
        System.out.println("10. Back");
    }

     // Sets safe default values that are used if no config file exists

    private void setDefaultConfig() {
        baseFares = new HashMap<>();
        discountRates = new HashMap<>();
        dailyCaps = new HashMap<>();

        peakStart = "06:30";
        peakEnd = "09:30";

        discountRates.put(Journey.PassengerType.ADULT, money("0.00"));
        discountRates.put(Journey.PassengerType.STUDENT, money("0.25"));
        discountRates.put(Journey.PassengerType.CHILD, money("0.50"));
        discountRates.put(Journey.PassengerType.SENIOR_CITIZEN, money("0.30"));

        dailyCaps.put(Journey.PassengerType.ADULT, money("8.00"));
        dailyCaps.put(Journey.PassengerType.STUDENT, money("6.00"));
        dailyCaps.put(Journey.PassengerType.CHILD, money("4.00"));
        dailyCaps.put(Journey.PassengerType.SENIOR_CITIZEN, money("7.00"));

        putDefaultFare(1, 1, Journey.TimeBand.PEAK, "2.50");
        putDefaultFare(1, 2, Journey.TimeBand.PEAK, "3.20");
        putDefaultFare(1, 3, Journey.TimeBand.PEAK, "3.80");
        putDefaultFare(1, 4, Journey.TimeBand.PEAK, "4.40");
        putDefaultFare(1, 5, Journey.TimeBand.PEAK, "5.00");
        putDefaultFare(2, 1, Journey.TimeBand.PEAK, "3.20");
        putDefaultFare(2, 2, Journey.TimeBand.PEAK, "2.30");
        putDefaultFare(2, 3, Journey.TimeBand.PEAK, "3.10");
        putDefaultFare(2, 4, Journey.TimeBand.PEAK, "3.80");
        putDefaultFare(2, 5, Journey.TimeBand.PEAK, "4.50");
        putDefaultFare(3, 1, Journey.TimeBand.PEAK, "3.80");
        putDefaultFare(3, 2, Journey.TimeBand.PEAK, "3.10");
        putDefaultFare(3, 3, Journey.TimeBand.PEAK, "2.10");
        putDefaultFare(3, 4, Journey.TimeBand.PEAK, "3.00");
        putDefaultFare(3, 5, Journey.TimeBand.PEAK, "3.70");
        putDefaultFare(4, 1, Journey.TimeBand.PEAK, "4.40");
        putDefaultFare(4, 2, Journey.TimeBand.PEAK, "3.80");
        putDefaultFare(4, 3, Journey.TimeBand.PEAK, "3.00");
        putDefaultFare(4, 4, Journey.TimeBand.PEAK, "2.00");
        putDefaultFare(4, 5, Journey.TimeBand.PEAK, "2.90");
        putDefaultFare(5, 1, Journey.TimeBand.PEAK, "5.00");
        putDefaultFare(5, 2, Journey.TimeBand.PEAK, "4.50");
        putDefaultFare(5, 3, Journey.TimeBand.PEAK, "3.70");
        putDefaultFare(5, 4, Journey.TimeBand.PEAK, "2.90");
        putDefaultFare(5, 5, Journey.TimeBand.PEAK, "1.90");

        putDefaultFare(1, 1, Journey.TimeBand.OFF_PEAK, "2.00");
        putDefaultFare(1, 2, Journey.TimeBand.OFF_PEAK, "2.70");
        putDefaultFare(1, 3, Journey.TimeBand.OFF_PEAK, "3.20");
        putDefaultFare(1, 4, Journey.TimeBand.OFF_PEAK, "3.70");
        putDefaultFare(1, 5, Journey.TimeBand.OFF_PEAK, "4.20");
        putDefaultFare(2, 1, Journey.TimeBand.OFF_PEAK, "2.70");
        putDefaultFare(2, 2, Journey.TimeBand.OFF_PEAK, "1.90");
        putDefaultFare(2, 3, Journey.TimeBand.OFF_PEAK, "2.60");
        putDefaultFare(2, 4, Journey.TimeBand.OFF_PEAK, "3.20");
        putDefaultFare(2, 5, Journey.TimeBand.OFF_PEAK, "3.80");
        putDefaultFare(3, 1, Journey.TimeBand.OFF_PEAK, "3.20");
        putDefaultFare(3, 2, Journey.TimeBand.OFF_PEAK, "2.60");
        putDefaultFare(3, 3, Journey.TimeBand.OFF_PEAK, "1.70");
        putDefaultFare(3, 4, Journey.TimeBand.OFF_PEAK, "2.50");
        putDefaultFare(3, 5, Journey.TimeBand.OFF_PEAK, "3.10");
        putDefaultFare(4, 1, Journey.TimeBand.OFF_PEAK, "3.70");
        putDefaultFare(4, 2, Journey.TimeBand.OFF_PEAK, "3.20");
        putDefaultFare(4, 3, Journey.TimeBand.OFF_PEAK, "2.50");
        putDefaultFare(4, 4, Journey.TimeBand.OFF_PEAK, "1.60");
        putDefaultFare(4, 5, Journey.TimeBand.OFF_PEAK, "2.40");
        putDefaultFare(5, 1, Journey.TimeBand.OFF_PEAK, "4.20");
        putDefaultFare(5, 2, Journey.TimeBand.OFF_PEAK, "3.80");
        putDefaultFare(5, 3, Journey.TimeBand.OFF_PEAK, "3.10");
        putDefaultFare(5, 4, Journey.TimeBand.OFF_PEAK, "2.40");
        putDefaultFare(5, 5, Journey.TimeBand.OFF_PEAK, "1.50");
    
    }
     private void putDefaultFare(int fromZone, int toZone, Journey.TimeBand timeBand, String value) {
        baseFares.put(buildFareKey(fromZone, toZone, timeBand), money(value));
    }

    private String buildFareKey(int fromZone, int toZone, Journey.TimeBand timeBand) {
        return fromZone + "-" + toZone + "-" + timeBand;
    }

    private BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasActiveProfile() {
        return !activeName.isEmpty() && activePassengerType != null && !activeDefaultPayment.isEmpty();
    }

    private void createOrChangeProfile() {
        System.out.println("\n--- Create/Change Profile ---");
        String name = readNonBlank("Enter rider name: ");
        Journey.PassengerType passengerType = readPassengerType();
        String paymentMethod = readPaymentMethod();

        activeName = name;
        activePassengerType = passengerType;
        activeDefaultPayment = paymentMethod;

        System.out.println("Active profile updated!");
    }

    private void viewActiveProfile() {
        if (!hasActiveProfile()) {
            System.out.println("No active profile, create or load one first.");
            return;
        }

        System.out.println("\n--- Active Profile ---");
        System.out.println("Name: " + activeName);
        System.out.println("Passenger Type: " + activePassengerType);
        System.out.println("Default Payment: " + activeDefaultPayment);
    }

     private void addJourney() {
        if (!hasActiveProfile()) {
            System.out.println("Create or load a rider profile first.");
            return;
        }

        System.out.println("\n--- Add Journey ---");
        String date = readDate("Enter date (dd/mm/yyyy, example 16/04/2026): ");
        String time = readTime("Enter time (HH:MM, example 08:30): ");
        int fromZone = readZone("Enter from zone (1-5, example 2): ");
        int toZone = readZone("Enter to zone (1-5, example 3): ");

        Journey.TimeBand timeBand = determineTimeBand(time);

        Journey journey = new Journey(
                nextId,
                activeName,
                activePassengerType,
                activeDefaultPayment,
                date,
                time,
                fromZone,
                toZone,
                timeBand
        );

        nextId++;
        journeys.add(journey);
        sortJourneysByDateAndTime();
        rebuildAllCharges();

        System.out.println("Journey added successfully.");
        System.out.println("Time band was calculated automatically as: " + timeBand);
        System.out.println("Base Fare: £" + journey.getBaseFare());
        System.out.println("Discount Applied: £" + journey.getDiscountAmount());
        System.out.println("Charged Fare: £" + journey.getChargedFare());
        System.out.println("Daily Cap Applied: " + (journey.isCapApplied() ? "YES" : "NO"));
        System.out.println("Current running total for that date/passenger: £" + getTotalForDateAndPassenger(date, journey.getPassengerType()));
    }

     private void editJourney() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        System.out.println("\n--- Edit Journey ---");
        int id = readInt("Enter journey ID to edit (example 1): ");
        Journey journey = findJourneyById(id);

        if (journey == null) {
            System.out.println("Error: Invalid ID. No journey found with ID " + id + ".");
            return;
        }

        String date = readDate("Enter new date (dd/mm/yyyy, example 16/04/2026): ");
        String time = readTime("Enter new time (HH:MM, example 18:45): ");
        int fromZone = readZone("Enter new from zone (1-5, example 1): ");
        int toZone = readZone("Enter new to zone (1-5, example 4): ");
        Journey.PassengerType passengerType = readPassengerType();

        journey.setDate(date);
        journey.setTime(time);
        journey.setFromZone(fromZone);
        journey.setToZone(toZone);
        journey.setPassengerType(passengerType);
        journey.setTimeBand(determineTimeBand(time));

        sortJourneysByDateAndTime();
        rebuildAllCharges();
        System.out.println("Journey updated successfully.");
    }
    
