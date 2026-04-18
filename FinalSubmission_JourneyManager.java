import java.io.*;
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

