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

