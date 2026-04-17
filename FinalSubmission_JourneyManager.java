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


