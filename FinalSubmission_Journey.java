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
