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
    
    private void removeJourney() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        System.out.println("\n--- Delete Journey ---");
        int id = readInt("Enter journey ID to delete (example 1): ");
        Journey journey = findJourneyById(id);

        if (journey == null) {
            System.out.println("Error: Invalid ID. No journey found with ID " + id + ".");
            return;
        }

        lastDeletedJourney = journey;
        journeys.remove(journey);
        rebuildAllCharges();
        System.out.println("Journey deleted. Totals were recalculated.");
    }

    private void undoDelete() {
        if (lastDeletedJourney == null) {
            System.out.println("No deleted journey available to restore.");
            return;
        }

        journeys.add(lastDeletedJourney);
        sortJourneysByDateAndTime();
        rebuildAllCharges();
        System.out.println("Deleted journey restored successfully.");
        lastDeletedJourney = null;
    }

    private void displayJourneys() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        System.out.println("\n--- All Journeys ---");
        for (Journey journey : journeys) {
            journey.display();
        }
    }

    private void viewRunningTotals() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        String date = readDate("Enter date for totals (dd/mm/yyyy, example 16/04/2026): ");
        ArrayList<Journey> dailyJourneys = getJourneysByDate(date);

        if (dailyJourneys.isEmpty()) {
            System.out.println("No journeys found for that date.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;

        System.out.println("\n--- Running Totals ---");
        for (Journey journey : dailyJourneys) {
            total = total.add(journey.getChargedFare());
            System.out.println(
                    "Journey ID " + journey.getId() +
                            " | Charged £" + journey.getChargedFare() +
                            " | Running Total £" + total.setScale(2, RoundingMode.HALF_UP) +
                            " | Cap Applied " + (journey.isCapApplied() ? "YES" : "NO")
            );
        }

        showCapStatusForDate(date);
    }
    
     private void showDailySummary() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        String date = readDate("Enter date for summary (dd/mm/yyyy, example 16/04/2026): ");
        ArrayList<Journey> dailyJourneys = getJourneysByDate(date);

        if (dailyJourneys.isEmpty()) {
            System.out.println("No journeys found for that date.");
            return;
        }

        printSummaryForDate(date, dailyJourneys);
    }

    private void printSummaryForDate(String date, ArrayList<Journey> dailyJourneys) {
        BigDecimal totalCharged = BigDecimal.ZERO;
        BigDecimal totalUncapped = BigDecimal.ZERO;
        Journey mostExpensive = dailyJourneys.get(0);
        int peakCount = 0;
        int offPeakCount = 0;
        int[] zoneCount = new int[6];
        HashMap<String, Integer> routeCounts = new HashMap<>();

        for (Journey journey : dailyJourneys) {
            totalCharged = totalCharged.add(journey.getChargedFare());
            totalUncapped = totalUncapped.add(journey.getDiscountedFare());

            if (journey.getChargedFare().compareTo(mostExpensive.getChargedFare()) > 0) {
                mostExpensive = journey;
            }

            if (journey.getTimeBand() == Journey.TimeBand.PEAK) {
                peakCount++;
            } else {
                offPeakCount++;
            }

            zoneCount[journey.getFromZone()]++;
            zoneCount[journey.getToZone()]++;
            incrementRouteCount(routeCounts, journey.getZonePairKey());
        }

        BigDecimal average = totalCharged.divide(new BigDecimal(dailyJourneys.size()), 2, RoundingMode.HALF_UP);
        BigDecimal savings = totalUncapped.subtract(totalCharged).setScale(2, RoundingMode.HALF_UP);
        if (savings.compareTo(BigDecimal.ZERO) < 0) {
            savings = BigDecimal.ZERO;
        }

        System.out.println("\n--- Daily Summary ---");
        System.out.println("Date: " + date);
        System.out.println("Number of Journeys: " + dailyJourneys.size());
        System.out.println("Total Cost: £" + totalCharged.setScale(2, RoundingMode.HALF_UP));
        System.out.println("Average Cost Per Journey: £" + average);
        System.out.println("Most Expensive Journey: ID " + mostExpensive.getId() + " (£" + mostExpensive.getChargedFare() + ")");
        System.out.println("Savings Compared to Uncapped Fares: £" + savings);
        System.out.println("Peak Journeys: " + peakCount);
        System.out.println("Off-Peak Journeys: " + offPeakCount);

        System.out.println("\nCounts Per Zone:");
        for (int zone = 1; zone <= 5; zone++) {
            if (zoneCount[zone] > 0) {
                System.out.println("Zone " + zone + ": " + zoneCount[zone]);
            }
        }

        System.out.println("\nCounts Per Zone Pair:");
        for (String routeKey : routeCounts.keySet()) {
            System.out.println(routeKey + ": " + routeCounts.get(routeKey));
        }

        showCapStatusForDate(date);
    }

    private void incrementRouteCount(HashMap<String, Integer> routeCounts, String routeKey) {
        if (routeCounts.containsKey(routeKey)) {
            routeCounts.put(routeKey, routeCounts.get(routeKey) + 1);
        } else {
            routeCounts.put(routeKey, 1);
        }
    }

     private void showCapStatusForDate(String date) {
        System.out.println("\nCap Status:");

        for (Journey.PassengerType passengerType : Journey.PassengerType.values()) {
            BigDecimal total = getTotalForDateAndPassenger(date, passengerType);
            BigDecimal cap = getDailyCap(passengerType);

            if (total.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println(
                        passengerType + ": £" + total + " / £" + cap +
                                " | Cap Reached: " + (total.compareTo(cap) >= 0 ? "YES" : "NO")
                );
            }
        }
    }
private void exportSummaryTextReport() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        String date = readDate("Enter date for TXT report (dd/mm/yyyy, example 16/04/2026): ");
        ArrayList<Journey> dailyJourneys = getJourneysByDate(date);

        if (dailyJourneys.isEmpty()) {
            System.out.println("No journeys found for that date.");
            return;
        }

        ensureReportsFolderExists();
        String filename = buildReportFileName(date, "summary", "txt");

        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            writer.println("CityRide Lite - Daily Summary Report");
            writer.println("===================================");
            writer.println("Rider: " + activeName);
            writer.println("Passenger Type: " + activePassengerType);
            writer.println("Default Payment: " + activeDefaultPayment);
            writer.println("Date: " + date);
            writer.println();
            writer.println("Journey Line Items:");

            ArrayList<Journey> daily = getJourneysByDate(date);
            for (Journey journey : daily) {
                writer.println(
                        "ID " + journey.getId() +
                                " | " + journey.getDate() + " " + journey.getTime() +
                                " | Zones " + journey.getFromZone() + " -> " + journey.getToZone() +
                                " | Band " + journey.getTimeBand() +
                                " | Base £" + journey.getBaseFare() +
                                " | Discount £" + journey.getDiscountAmount() +
                                " | Charged £" + journey.getChargedFare() +
                                " | Cap Applied " + (journey.isCapApplied() ? "YES" : "NO")
                );
            }

            writer.println();
            writeSummaryBlock(writer, date, daily);
            writer.close();
            System.out.println("TXT summary report exported to " + filename);
        } catch (Exception e) {
            System.out.println("Error exporting TXT summary report.");
        }
    }

     private void exportSummaryCsvReport() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        String date = readDate("Enter date for CSV report (dd/mm/yyyy, example 16/04/2026): ");
        ArrayList<Journey> dailyJourneys = getJourneysByDate(date);

        if (dailyJourneys.isEmpty()) {
            System.out.println("No journeys found for that date.");
            return;
        }

        ensureReportsFolderExists();
        String filename = buildReportFileName(date, "summary", "csv");

        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            writer.println("id,riderName,passengerType,defaultPayment,date,time,fromZone,toZone,timeBand,zonesCrossed,baseFare,discountAmount,discountedFare,chargedFare,capApplied");

            for (Journey journey : dailyJourneys) {
                writer.println(
                        journey.getId() + "," +
                                safeCsv(journey.getRiderName()) + "," +
                                journey.getPassengerType() + "," +
                                safeCsv(journey.getDefaultPayment()) + "," +
                                journey.getDate() + "," +
                                journey.getTime() + "," +
                                journey.getFromZone() + "," +
                                journey.getToZone() + "," +
                                journey.getTimeBand() + "," +
                                journey.getZonesCrossed() + "," +
                                journey.getBaseFare() + "," +
                                journey.getDiscountAmount() + "," +
                                journey.getDiscountedFare() + "," +
                                journey.getChargedFare() + "," +
                                (journey.isCapApplied() ? "YES" : "NO")
                );
            }

            writer.println();
            writeSummaryCsvFooter(writer, date, dailyJourneys);
            writer.close();
            System.out.println("CSV summary report exported to " + filename);
        } catch (Exception e) {
            System.out.println("Error exporting CSV summary report.");
        }
    }

    private void writeSummaryBlock(PrintWriter writer, String date, ArrayList<Journey> dailyJourneys) {
        SummaryData data = buildSummaryData(date, dailyJourneys);

        writer.println("Summary:");
        writer.println("Number of Journeys: " + data.numberOfJourneys);
        writer.println("Total Cost: £" + data.totalCharged);
        writer.println("Average Cost Per Journey: £" + data.averageCost);
        writer.println("Most Expensive Journey: ID " + data.mostExpensiveJourneyId + " (£" + data.mostExpensiveFare + ")");
        writer.println("Savings Compared to Uncapped Fares: £" + data.savings);
        writer.println("Peak Journeys: " + data.peakCount);
        writer.println("Off-Peak Journeys: " + data.offPeakCount);
        writer.println();
        writer.println("Counts Per Zone:");
        for (int zone = 1; zone <= 5; zone++) {
            if (data.zoneCount[zone] > 0) {
                writer.println("Zone " + zone + ": " + data.zoneCount[zone]);
            }
        }
        writer.println();
        writer.println("Counts Per Zone Pair:");
        for (String routeKey : data.routeCounts.keySet()) {
            writer.println(routeKey + ": " + data.routeCounts.get(routeKey));
        }
        writer.println();
        writer.println("Cap Status:");
        for (Journey.PassengerType passengerType : Journey.PassengerType.values()) {
            BigDecimal total = getTotalForDateAndPassenger(date, passengerType);
            BigDecimal cap = getDailyCap(passengerType);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                writer.println(passengerType + ": £" + total + " / £" + cap + " | Cap Reached: " + (total.compareTo(cap) >= 0 ? "YES" : "NO"));
            }
        }
    }

    private void writeSummaryCsvFooter(PrintWriter writer, String date, ArrayList<Journey> dailyJourneys) {
        SummaryData data = buildSummaryData(date, dailyJourneys);

        writer.println("Total Journeys," + data.numberOfJourneys);
        writer.println("Total Cost,£" + data.totalCharged);
        writer.println("Average Cost Per Journey,£" + data.averageCost);
        writer.println("Most Expensive Journey,ID " + data.mostExpensiveJourneyId + " £" + data.mostExpensiveFare);
        writer.println("Savings Compared to Uncapped Fares,£" + data.savings);
        writer.println("Peak Journeys," + data.peakCount);
        writer.println("Off-Peak Journeys," + data.offPeakCount);

        for (int zone = 1; zone <= 5; zone++) {
            if (data.zoneCount[zone] > 0) {
                writer.println("Zone " + zone + " Count," + data.zoneCount[zone]);
            }
        }

        for (String routeKey : data.routeCounts.keySet()) {
            writer.println("Route " + routeKey + "," + data.routeCounts.get(routeKey));
        }

        for (Journey.PassengerType passengerType : Journey.PassengerType.values()) {
            BigDecimal total = getTotalForDateAndPassenger(date, passengerType);
            BigDecimal cap = getDailyCap(passengerType);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                writer.println(passengerType + " Cap Reached," + (total.compareTo(cap) >= 0 ? "YES" : "NO"));
            }
        }
    }

    private SummaryData buildSummaryData(String date, ArrayList<Journey> dailyJourneys) {
        SummaryData data = new SummaryData();
        data.numberOfJourneys = dailyJourneys.size();
        data.zoneCount = new int[6];
        data.routeCounts = new HashMap<>();
        Journey mostExpensive = dailyJourneys.get(0);

        for (Journey journey : dailyJourneys) {
            data.totalCharged = data.totalCharged.add(journey.getChargedFare());
            data.totalUncapped = data.totalUncapped.add(journey.getDiscountedFare());

            if (journey.getTimeBand() == Journey.TimeBand.PEAK) {
                data.peakCount++;
            } else {
                data.offPeakCount++;
            }

            if (journey.getChargedFare().compareTo(mostExpensive.getChargedFare()) > 0) {
                mostExpensive = journey;
            }

            data.zoneCount[journey.getFromZone()]++;
            data.zoneCount[journey.getToZone()]++;
            incrementRouteCount(data.routeCounts, journey.getZonePairKey());
        }

        data.averageCost = data.totalCharged.divide(new BigDecimal(data.numberOfJourneys), 2, RoundingMode.HALF_UP);
        data.savings = data.totalUncapped.subtract(data.totalCharged).setScale(2, RoundingMode.HALF_UP);
        if (data.savings.compareTo(BigDecimal.ZERO) < 0) {
            data.savings = BigDecimal.ZERO;
        }

        data.totalCharged = data.totalCharged.setScale(2, RoundingMode.HALF_UP);
        data.mostExpensiveJourneyId = mostExpensive.getId();
        data.mostExpensiveFare = mostExpensive.getChargedFare();
        return data;
    }

    private void importJourneysFromCsv() {
        String filename = readNonBlank("Enter CSV filename to import (example journeys.csv): ");
        File file = new File(filename);

        if (!file.exists()) {
            System.out.println("Error: File not found - " + filename);
            return;
        }

        int importedCount = 0;
        int errorCount = 0;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    errorCount++;
                    continue;
                }

                try {
                    int id = nextId;
                    String riderName = parts[1].trim();
                    Journey.PassengerType passengerType = Journey.PassengerType.fromString(parts[2].trim());
                    String date = validateDateValue(parts[3].trim());
                    String time = validateTimeValue(parts[4].trim());
                    int fromZone = validateZoneValue(parts[5].trim());
                    int toZone = validateZoneValue(parts[6].trim());
                    Journey.TimeBand timeBand = determineTimeBand(time);
                    String defaultPayment = hasActiveProfile() ? activeDefaultPayment : "CONTACTLESS_CARD";

                    Journey journey = new Journey(id, riderName, passengerType, defaultPayment, date, time, fromZone, toZone, timeBand);
                    journeys.add(journey);
                    nextId++;
                    importedCount++;
                } catch (Exception e) {
                    errorCount++;
                }
            }

            reader.close();
            sortJourneysByDateAndTime();
            rebuildAllCharges();
            System.out.println(importedCount + " journeys imported.");
            if (errorCount > 0) {
                System.out.println(errorCount + " rows had errors and were skipped.");
            }
        } catch (Exception e) {
            System.out.println("Error importing journeys.");
        }
    }

    private void exportJourneysBackup() {
        if (journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        String filename = readNonBlank("Enter backup CSV filename (example my_journeys_backup.csv): ");

        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            writer.println("id,riderName,passengerType,date,time,fromZone,toZone,timeBand");
            for (Journey journey : journeys) {
                writer.println(
                        journey.getId() + "," +
                                safeCsv(journey.getRiderName()) + "," +
                                journey.getPassengerType() + "," +
                                journey.getDate() + "," +
                                journey.getTime() + "," +
                                journey.getFromZone() + "," +
                                journey.getToZone() + "," +
                                journey.getTimeBand()
                );
            }
            writer.close();
            System.out.println("Journeys exported to " + filename);
        } catch (Exception e) {
            System.out.println("Error exporting journeys.");
        }
    }

     private void saveCurrentState() {
        saveProfile();
        saveJourneys();
        System.out.println("Current day state saved (profile + journeys).");
    }

    private void exitProgram() {
        System.out.println("\n--- Exit ---");
        System.out.print("Would you like to save the rider's current day state (profile + journeys)? (yes/no): ");
        String answer = scanner.nextLine().trim().toLowerCase();

        if (answer.equals("yes") || answer.equals("y")) {
            saveCurrentState();
        }

        System.out.print("Would you like to save the active config as well? (yes/no): ");
        String configAnswer = scanner.nextLine().trim().toLowerCase();
        if (configAnswer.equals("yes") || configAnswer.equals("y")) {
            saveConfig();
        }

        System.out.println("Program ended.");
    }

    private Journey.TimeBand determineTimeBand(String timeText) {
        LocalTime journeyTime = LocalTime.parse(timeText, TIME_FORMATTER);
        LocalTime peakStartTime = LocalTime.parse(peakStart, TIME_FORMATTER);
        LocalTime peakEndTime = LocalTime.parse(peakEnd, TIME_FORMATTER);

        if ((!journeyTime.isBefore(peakStartTime)) && (!journeyTime.isAfter(peakEndTime))) {
            return Journey.TimeBand.PEAK;
        }
        return Journey.TimeBand.OFF_PEAK;
    }

    // Rebuilds every fare after changes
    // This ensures edits, imports, deletes and admin config changes all remain accurate

    private void rebuildAllCharges() {
        HashMap<String, BigDecimal> totalsByDateAndPassenger = new HashMap<>();
        sortJourneysByDateAndTime();

        for (Journey journey : journeys) {
            calculateJourneyValues(journey);

            String totalKey = journey.getDate() + "|" + journey.getPassengerType();
            BigDecimal currentTotal = totalsByDateAndPassenger.getOrDefault(totalKey, BigDecimal.ZERO);
            BigDecimal cap = getDailyCap(journey.getPassengerType());
            BigDecimal discountedFare = journey.getDiscountedFare();
            BigDecimal chargedFare;
            boolean capApplied;

            if (currentTotal.compareTo(cap) >= 0) {
                chargedFare = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                capApplied = true;
            } else if (currentTotal.add(discountedFare).compareTo(cap) > 0) {
                chargedFare = cap.subtract(currentTotal).setScale(2, RoundingMode.HALF_UP);
                capApplied = true;
            } else {
                chargedFare = discountedFare;
                capApplied = false;
            }

            journey.setChargedFare(chargedFare);
            journey.setCapApplied(capApplied);
            totalsByDateAndPassenger.put(totalKey, currentTotal.add(chargedFare));
        }
    }

     private void calculateJourneyValues(Journey journey) {
        BigDecimal baseFare = getBaseFare(journey.getFromZone(), journey.getToZone(), journey.getTimeBand());
        BigDecimal discountRate = getDiscountRate(journey.getPassengerType());
        BigDecimal discountAmount = baseFare.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountedFare = baseFare.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        journey.setZonesCrossed(Math.abs(journey.getToZone() - journey.getFromZone()) + 1);
        journey.setBaseFare(baseFare);
        journey.setDiscountAmount(discountAmount);
        journey.setDiscountedFare(discountedFare);
    }

    private BigDecimal getBaseFare(int fromZone, int toZone, Journey.TimeBand timeBand) {
        return baseFares.getOrDefault(buildFareKey(fromZone, toZone, timeBand), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getDiscountRate(Journey.PassengerType passengerType) {
        return discountRates.getOrDefault(passengerType, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getDailyCap(Journey.PassengerType passengerType) {
        return dailyCaps.getOrDefault(passengerType, money("999.99")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getTotalForDateAndPassenger(String date, Journey.PassengerType passengerType) {
        BigDecimal total = BigDecimal.ZERO;
        for (Journey journey : journeys) {
            if (journey.getDate().equals(date) && journey.getPassengerType() == passengerType) {
                total = total.add(journey.getChargedFare());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private ArrayList<Journey> getJourneysByDate(String date) {
        ArrayList<Journey> dailyJourneys = new ArrayList<>();
        for (Journey journey : journeys) {
            if (journey.getDate().equals(date)) {
                dailyJourneys.add(journey);
            }
        }
        return dailyJourneys;
    }

    private Journey findJourneyById(int id) {
        for (Journey journey : journeys) {
            if (journey.getId() == id) {
                return journey;
            }
        }
        return null;
    }

    private void sortJourneysByDateAndTime() {
        boolean swapped = true;

        while (swapped) {
            swapped = false;

            for (int index = 0; index < journeys.size() - 1; index++) {
                Journey currentJourney = journeys.get(index);
                Journey nextJourney = journeys.get(index + 1);

                if (compareJourneysByDateAndTime(currentJourney, nextJourney) > 0) {
                    Collections.swap(journeys, index, index + 1);
                    swapped = true;
                }
            }
        }
    }

    private int compareJourneysByDateAndTime(Journey firstJourney, Journey secondJourney) {
        LocalDate firstDate = LocalDate.parse(firstJourney.getDate(), DATE_FORMATTER);
        LocalDate secondDate = LocalDate.parse(secondJourney.getDate(), DATE_FORMATTER);
        int dateComparison = firstDate.compareTo(secondDate);

        if (dateComparison != 0) {
            return dateComparison;
        }

        LocalTime firstTime = LocalTime.parse(firstJourney.getTime(), TIME_FORMATTER);
        LocalTime secondTime = LocalTime.parse(secondJourney.getTime(), TIME_FORMATTER);
        return firstTime.compareTo(secondTime);
    }

     private void viewConfig() {
        System.out.println("\n--- Active Config ---");
        System.out.println("Peak Window: " + peakStart + " to " + peakEnd);

        System.out.println("\nDiscount Rates:");
        for (Journey.PassengerType passengerType : Journey.PassengerType.values()) {
            System.out.println(passengerType + ": " + discountRates.getOrDefault(passengerType, BigDecimal.ZERO));
        }

        System.out.println("\nDaily Caps:");
        for (Journey.PassengerType passengerType : Journey.PassengerType.values()) {
            System.out.println(passengerType + ": £" + dailyCaps.getOrDefault(passengerType, BigDecimal.ZERO));
        }

        System.out.println("\nSample Base Fares:");
        System.out.println("1 -> 2 PEAK: £" + baseFares.getOrDefault(buildFareKey(1, 2, Journey.TimeBand.PEAK), BigDecimal.ZERO));
        System.out.println("1 -> 2 OFF_PEAK: £" + baseFares.getOrDefault(buildFareKey(1, 2, Journey.TimeBand.OFF_PEAK), BigDecimal.ZERO));
    }

     private void updateDiscount() {
        Journey.PassengerType passengerType = readPassengerType();
        BigDecimal value = readDecimalInRange("Enter new discount rate (0.00 to 1.00, example 0.25): ", BigDecimal.ZERO, BigDecimal.ONE);

        if (value == null) {
            System.out.println("Validation failed. Discount not saved.");
            return;
        }

        discountRates.put(passengerType, value);
        rebuildAllCharges();
        System.out.println("Discount updated successfully.");
    }

    private void deleteDiscount() {
        Journey.PassengerType passengerType = readPassengerType();
        discountRates.put(passengerType, money("0.00"));
        rebuildAllCharges();
        System.out.println("Discount reset to 0.00 for " + passengerType + ".");
    }

    private void updateDailyCap() {
        Journey.PassengerType passengerType = readPassengerType();
        BigDecimal value = readPositiveDecimal("Enter new daily cap (example 8.00): ");

        if (value == null) {
            System.out.println("Validation failed. Daily cap not saved.");
            return;
        }

        dailyCaps.put(passengerType, value);
        rebuildAllCharges();
        System.out.println("Daily cap updated successfully.");
    }

    private void deleteDailyCap() {
        Journey.PassengerType passengerType = readPassengerType();
        dailyCaps.remove(passengerType);
        rebuildAllCharges();
        System.out.println("Daily cap deleted for " + passengerType + ".");
    }

    private void updateBaseFare() {
        int fromZone = readZone("Enter from zone (1-5, example 1): ");
        int toZone = readZone("Enter to zone (1-5, example 4): ");
        Journey.TimeBand timeBand = readTimeBand();
        BigDecimal amount = readPositiveDecimal("Enter new base fare (example 3.80): ");

        if (amount == null) {
            System.out.println("Validation failed. Base fare not saved.");
            return;
        }

        baseFares.put(buildFareKey(fromZone, toZone, timeBand), amount);
        rebuildAllCharges();
        System.out.println("Base fare updated successfully.");
    }

    private void deleteBaseFare() {
        int fromZone = readZone("Enter from zone (1-5, example 1): ");
        int toZone = readZone("Enter to zone (1-5, example 4): ");
        Journey.TimeBand timeBand = readTimeBand();
        String key = buildFareKey(fromZone, toZone, timeBand);

        if (!baseFares.containsKey(key)) {
            System.out.println("Error: No base fare exists for that zone pair and band. Nothing deleted.");
            return;
        }

        baseFares.remove(key);
        rebuildAllCharges();
        System.out.println("Base fare deleted successfully.");
    }

    private void updatePeakWindow() {
        String newStart = readTime("Enter new peak start time (HH:MM, example 06:30): ");
        String newEnd = readTime("Enter new peak end time (HH:MM, example 09:30): ");

        LocalTime start = LocalTime.parse(newStart, TIME_FORMATTER);
        LocalTime end = LocalTime.parse(newEnd, TIME_FORMATTER);

        if (!start.isBefore(end)) {
            System.out.println("Validation failed. Peak start must be before peak end. Changes not saved.");
            return;
        }

        peakStart = newStart;
        peakEnd = newEnd;
        updateAllJourneyBandsFromTimes();
        rebuildAllCharges();
        System.out.println("Peak window updated successfully.");
    }

    private void updateAllJourneyBandsFromTimes() {
        for (Journey journey : journeys) {
            journey.setTimeBand(determineTimeBand(journey.getTime()));
        }
    }

    private void loadJourneys() {
        journeys = new ArrayList<>();
        File file = new File(JOURNEYS_FILE);

        if (!file.exists()) {
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            int maxId = 0;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    continue;
                }

                int id = Integer.parseInt(parts[0].trim());
                String riderName = parts[1].trim();
                Journey.PassengerType passengerType = Journey.PassengerType.fromString(parts[2].trim());
                String date = validateDateValue(parts[3].trim());
                String time = validateTimeValue(parts[4].trim());
                int fromZone = validateZoneValue(parts[5].trim());
                int toZone = validateZoneValue(parts[6].trim());
                Journey.TimeBand timeBand = determineTimeBand(time);
                String defaultPayment = hasActiveProfile() ? activeDefaultPayment : "CONTACTLESS_CARD";

                Journey journey = new Journey(id, riderName, passengerType, defaultPayment, date, time, fromZone, toZone, timeBand);
                journeys.add(journey);

                if (id > maxId) {
                    maxId = id;
                }
            }

            reader.close();
            nextId = maxId + 1;
            sortJourneysByDateAndTime();
        } catch (Exception e) {
            System.out.println("Error loading journeys. Starting with an empty journey list.");
            journeys = new ArrayList<>();
            nextId = 1;
        }
    }

    private void saveJourneys() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(JOURNEYS_FILE));
            writer.println("id,riderName,passengerType,date,time,fromZone,toZone,timeBand");
            for (Journey journey : journeys) {
                writer.println(
                        journey.getId() + "," +
                                safeCsv(journey.getRiderName()) + "," +
                                journey.getPassengerType() + "," +
                                journey.getDate() + "," +
                                journey.getTime() + "," +
                                journey.getFromZone() + "," +
                                journey.getToZone() + "," +
                                journey.getTimeBand()
                );
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Error saving journeys.");
        }
    }

     private void loadProfile() {
        File file = new File(PROFILE_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            String json = readWholeFile(PROFILE_FILE);
            String name = getJsonValue(json, "name");
            String passengerType = getJsonValue(json, "passengerType");
            String defaultPayment = getJsonValue(json, "defaultPayment");

            if (name != null) {
                activeName = name;
            }
            if (passengerType != null) {
                activePassengerType = Journey.PassengerType.fromString(passengerType);
            }
            if (defaultPayment != null) {
                activeDefaultPayment = defaultPayment;
            }
        } catch (Exception e) {
            System.out.println("Could not load profile. Starting without an active profile.");
        }
    }

    private void saveProfile() {
        if (!hasActiveProfile()) {
            System.out.println("No active profile to save.");
            return;
        }

        try {
            PrintWriter writer = new PrintWriter(new FileWriter(PROFILE_FILE));
            writer.println("{");
            writer.println("  \"name\": \"" + escapeJson(activeName) + "\",");
            writer.println("  \"passengerType\": \"" + activePassengerType + "\",");
            writer.println("  \"defaultPayment\": \"" + escapeJson(activeDefaultPayment) + "\"");
            writer.println("}");
            writer.close();
        } catch (Exception e) {
            System.out.println("Error saving profile.");
        }
    }
