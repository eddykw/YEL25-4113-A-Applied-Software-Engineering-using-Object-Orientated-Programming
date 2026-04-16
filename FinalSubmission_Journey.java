import java.math.BigDecimal;

public class Journey {

    public enum PassengerType {
        ADULT,
        STUDENT,
        CHILD,
        SENIOR_CITIZEN;

        public static PassengerType fromString(String value) {
            value = value.trim().toUpperCase().replace(' ', '_');
            if (value.equals("SENIOR")) {
                value = "SENIOR_CITIZEN";
            }
            return PassengerType.valueOf(value);
        }
    }

     public enum PaymentMethod {
        CONTACTLESS_CARD,
        TRAVEL_PREPAID_CARD,
        MOBILE_PAYMENT,
        OTHER;

        public static PaymentMethod fromString(String value) {
            value = value.trim().toUpperCase().replace(' ', '_');
            return PaymentMethod.valueOf(value);
        }
    }

     public enum TimeBand {
         public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getFromZone() {
        return fromZone;
    }

    public void setFromZone(int fromZone) {
        this.fromZone = fromZone;
        updateZonesCrossed();
    }
        PEAK,
        OFF_PEAK;

        public static TimeBand fromString(String value) {
            value = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            if (value.equals("OFFPEAK")) {
                value = "OFF_PEAK";
            }
            return TimeBand.valueOf(value);
        }
    }

    private int id;
    private String riderName;
    private PassengerType passengerType;
    private String defaultPayment;
    private String date;
    private String time;
    private int fromZone;
    private int toZone;
    private TimeBand timeBand;
    private int zonesCrossed;
    private BigDecimal baseFare;
    private BigDecimal discountAmount;
    private BigDecimal discountedFare;
    private BigDecimal chargedFare;
    private boolean capApplied;

    public Journey() {
    }

    public Journey(int id,
                   String riderName,
                   PassengerType passengerType,
                   String defaultPayment,
                   String date,
                   String time,
                   int fromZone,
                   int toZone,
                   TimeBand timeBand) {
        this.id = id;
        this.riderName = riderName;
        this.passengerType = passengerType;
        this.defaultPayment = defaultPayment;
        this.date = date;
        this.time = time;
        this.fromZone = fromZone;
        this.toZone = toZone;
        this.timeBand = timeBand;
        this.zonesCrossed = Math.abs(toZone - fromZone) + 1;
        this.baseFare = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.discountedFare = BigDecimal.ZERO;
        this.chargedFare = BigDecimal.ZERO;
        this.capApplied = false;
    }

 public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRiderName() {
        return riderName;
    }

    public void setRiderName(String riderName) {
        this.riderName = riderName;
    }

  public PassengerType getPassengerType() {
        return passengerType;
    }

    public void setPassengerType(PassengerType passengerType) {
        this.passengerType = passengerType;
    }

    public String getDefaultPayment() {
        return defaultPayment;
    }

    public void setDefaultPayment(String defaultPayment) {
        this.defaultPayment = defaultPayment;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getFromZone() {
        return fromZone;
    }

    public void setFromZone(int fromZone) {
        this.fromZone = fromZone;
        updateZonesCrossed();
    }
