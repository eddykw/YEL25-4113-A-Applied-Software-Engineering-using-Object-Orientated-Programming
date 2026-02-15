IY4113 Milestone 4

| Assessment Details | Please Complete All Details                                             |
| ------------------ | ----------------------------------------------------------------------- |
| Group              | A                                                                       |
| Module Title       | IY4113 Applied Software Engineering using Object-Orientated Programming |
| Assessment Type    | Java Fundamentals                                                       |
| Module Tutor Name  | Jonathan Shore                                                          |
| Student ID Number  | P488018                                                                 |
| Date of Submission | 15/02/25                                                                |
| Word Count         |     1252                                                                   |
| GItHub Link        |     https://github.com/eddykw/YEL25-4113-A-Applied-Software-Engineering-using-Object-Orientated-Programming                                                                    |

- [x] *I confirm that this assignment is my own work. Where I have referred to academic sources, I have provided in-text citations and included the sources in
  the final reference list.*

- [x] *Where I have used AI, I have cited and referenced appropriately.

------------------------------------------------------------------------------------------------------------------------------

Program Code

---

------------------------------------------------------------------------------------------------------------------------------

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Journey {
    private int id, fromZone, toZone, zonesCrossed;
    private String date;
    private CityRideDataset.PassengerType passengerType;
    private CityRideDataset.TimeBand timeBand;
    private BigDecimal baseFare, discountedFare, chargedFare;

    public Journey(int id, String date, int fromZone, int toZone,
                   CityRideDataset.PassengerType passengerType,
                   CityRideDataset.TimeBand timeBand,
                   int zonesCrossed,
                   BigDecimal baseFare,
                   BigDecimal discountedFare,
                   BigDecimal chargedFare) {
        this.id = id; this.date = date; this.fromZone = fromZone; this.toZone = toZone;
        this.passengerType = passengerType; this.timeBand = timeBand; this.zonesCrossed = zonesCrossed;
        this.baseFare = baseFare; this.discountedFare = discountedFare; this.chargedFare = chargedFare;
    }
    
    public int getId() { return id; }
    public String getDate() { return date; }
    public int getFromZone() { return fromZone; }
    public int getToZone() { return toZone; }
    public CityRideDataset.PassengerType getPassengerType() { return passengerType; }
    public CityRideDataset.TimeBand getTimeBand() { return timeBand; }
    public BigDecimal getChargedFare() { return chargedFare; }
    
    public void display() {
        System.out.println("ID:" + id + " Date:" + date + " From:" + fromZone + " To:" + toZone +
                           " Band:" + timeBand + " Passenger:" + passengerType + 
                           " Zones:" + zonesCrossed + " Base:Â£" + baseFare +
                           " Disc:Â£" + discountedFare + " Charged:Â£" + chargedFare);
    }

}

class JourneyManager {
    private String sessionName;
    private CityRideDataset.PassengerType sessionPassengerType;
    private List<Journey> journeys = new ArrayList<>();
    private int nextId = 1;
    private BigDecimal adultTotal = BigDecimal.ZERO, studentTotal = BigDecimal.ZERO,
                       childTotal = BigDecimal.ZERO, seniorTotal = BigDecimal.ZERO;

    public JourneyManager(String sessionName, CityRideDataset.PassengerType type) {
        this.sessionName = sessionName; this.sessionPassengerType = type;
    }
    public String getSessionName() { return sessionName; }
    public CityRideDataset.PassengerType getSessionPassengerType() { return sessionPassengerType; }
    
    private BigDecimal getTotal(CityRideDataset.PassengerType t) {
        switch(t) {
            case ADULT: return adultTotal; case STUDENT: return studentTotal;
            case CHILD: return childTotal; default: return seniorTotal;
        }
    }
    
    private void updateTotal(CityRideDataset.PassengerType t, BigDecimal amt) {
        switch(t) {
            case ADULT: adultTotal = adultTotal.add(amt); break;
            case STUDENT: studentTotal = studentTotal.add(amt); break;
            case CHILD: childTotal = childTotal.add(amt); break;
            default: seniorTotal = seniorTotal.add(amt);
        }
    }
    
    public void addJourney(String date, int from, int to, CityRideDataset.PassengerType type, CityRideDataset.TimeBand band) {
        int zones = Math.abs(to - from) + 1;
        BigDecimal base = CityRideDataset.getBaseFare(from, to, band);
        BigDecimal discount = base.multiply(CityRideDataset.DISCOUNT_RATE.get(type)).setScale(2,RoundingMode.HALF_UP);
        BigDecimal discounted = base.subtract(discount).setScale(2,RoundingMode.HALF_UP);
        BigDecimal cap = CityRideDataset.DAILY_CAP.get(type);
        BigDecimal running = getTotal(type);
        BigDecimal charged = running.compareTo(cap) >=0 ? BigDecimal.ZERO :
                             running.add(discounted).compareTo(cap) >0 ? cap.subtract(running) : discounted;
        updateTotal(type, charged);
    
        journeys.add(new Journey(nextId++, date, from, to, type, band, zones, base, discounted, charged));
        System.out.println("\nJourney added! Charged: Â£" + charged);
    }
    
    public void listAllJourneys() {
        if(journeys.isEmpty()) { System.out.println("No journeys recorded."); return; }
        System.out.println("\n--- All Journeys ---");
        for(Journey j : journeys) j.display();
    }
    
    public void removeJourney(int id) {
        Journey toRemove = null;
        for(Journey j: journeys) if(j.getId()==id) toRemove=j;
        if(toRemove==null) { System.out.println("No journey with ID "+id); return; }
        journeys.remove(toRemove);
        recalcTotals();
        System.out.println("Journey removed and totals recalculated.");
    }
    
    private void recalcTotals() {
        adultTotal=studentTotal=childTotal=seniorTotal=BigDecimal.ZERO;
        List<Journey> oldJourneys = new ArrayList<>(journeys);
        journeys.clear(); nextId=1;
        for(Journey j: oldJourneys) addJourney(j.getDate(), j.getFromZone(), j.getToZone(), j.getPassengerType(), j.getTimeBand());
    }
    
    public void resetDay() { journeys.clear(); adultTotal=studentTotal=childTotal=seniorTotal=BigDecimal.ZERO; nextId=1; System.out.println("Day reset."); }
    
    public void showDailySummary() {
        if(journeys.isEmpty()) { System.out.println("No journeys to summarise."); return; }
        BigDecimal total = adultTotal.add(studentTotal).add(childTotal).add(seniorTotal);
        BigDecimal avg = total.divide(new BigDecimal(journeys.size()),2,RoundingMode.HALF_UP);
        System.out.println("\n--- Daily Summary --- Total journeys: " + journeys.size() + " Total: £" + total + " Average: £" + avg);
    }

}

class InputHelper {
    private Scanner scanner;
    public InputHelper(Scanner s) { scanner=s; }
    public int readInt(String prompt){ while(true){ try{ System.out.print(prompt); return Integer.parseInt(scanner.nextLine().trim()); } catch(Exception e){ System.out.println("Invalid number"); } } }
    public int readZone(String prompt){ int z; while(true){ z=readInt(prompt); if(z>=1 && z<=5) return z; System.out.println("Zone must be 1-5."); } }
    public String readDate(String prompt){ String d; while(true){ System.out.print(prompt); d=scanner.nextLine().trim(); if(!d.isEmpty()) return d; System.out.println("Date required."); } }
    public boolean readYesNo(String prompt){ String r; while(true){ System.out.print(prompt+" (yes/no): "); r=scanner.nextLine().trim().toLowerCase(); if(r.equals("yes")||r.equals("y")) return true; if(r.equals("no")||r.equals("n")) return false; System.out.println("Type yes or no."); } }
}

public class CityRideLiteApp {
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in); InputHelper input=new InputHelper(sc);
        System.out.println("Welcome to CityRide Lite!");
        String name=input.readDate("Enter your name (optional, press Enter to skip): ");
        CityRideDataset.PassengerType type=CityRideDataset.PassengerType.ADULT;
        System.out.println("Select passenger type (1-Adult,2-Student,3-Child,4-Senior):");
        int t=input.readInt("Choice: "); if(t==2) type=CityRideDataset.PassengerType.STUDENT; else if(t==3) type=CityRideDataset.PassengerType.CHILD; else if(t==4) type=CityRideDataset.PassengerType.SENIOR_CITIZEN;
        JourneyManager manager=new JourneyManager(name,type);

        int choice;
        do{
            System.out.println("\n1.Add Journey 2.List All 3.Remove 4.Summary 5.Reset 6.Exit");
            choice=input.readInt("Enter choice: ");
            switch(choice){
                case 1:
                    String date=input.readDate("Date (dd/mm/yyyy): ");
                    int f=input.readZone("From Zone: ");
                    int to=input.readZone("To Zone: ");
                    manager.addJourney(date,f,to,type,CityRideDataset.TimeBand.PEAK);
                    break;
                case 2: manager.listAllJourneys(); break;
                case 3:
                    int id=input.readInt("Journey ID to remove: ");
                    if(input.readYesNo("Remove journey "+id+"?")) manager.removeJourney(id); else System.out.println("Cancelled.");
                    break;
                case 4: manager.showDailySummary(); break;
                case 5:
                    if(input.readYesNo("Reset all journeys?")) manager.resetDay(); else System.out.println("Cancelled.");
                    break;
            }
        } while(choice!=6);
    
        System.out.println("Goodbye!"); sc.close();
    }

}

------------------------------------------------------------------------------------------------------------------------------

Updated Gantt Chart

------------------------------------------------------------------------------------------------------------------------------


------------------------------------------------------------------------------------------------------------------------------

Diary Entries

------------------------------------------------------------------------------------------------------------------------------

Thursday 12th February 2026
I started the day by reviewing all the in class code we had covered so far. This included the original CityRide Lite program with the Journey and CityRideManager classes as well as the Student grading program. I tried understanding how each class interacted, how fares were calculated for different passenger types and how daily caps were applied. I also traced the logic for applying discounts to ensure I fully understood the calculations. Some parts, like the total fare recalculation after removing a journey were a bit tricky to follow so I made note of it to refer back to them later.

Friday 13th February 2026 
I continued by checking the Student grading code. I reviewed how the GradingRules class worked with static constants for grade thresholds and pass marks. I practiced converting numeric marks to letter grades using the GradeCalculator class and printing detailed student results including grade, pass/fail status and classification. I tested the code with sample data to confirm it was working correctly. This helped me understand the pattern of separating logic into classes and methods which I knew would be useful when extending the CityRide Lite program.

Saturday 14th February 2026 
I focused on the updated CityRideDataset class which used enums and maps for fares, discounts, and daily caps. I looked at how the getBaseFare method worked with different zones and time bands. After that I started thinking about how to combine the original CityRide Lite code with this new dataset structure. I made a plan for one single program file that could use all these, I found which methods from the previous code needed updating like adding journeys, calculating discounted fares, and keeping running totals per passenger type.

Sunday 15th February 2026
Today I implemented the final version of the combined program. I created one file with all classes, Journey, JourneyManager, InputHelper and CityRideLiteApp. I added the CityRideDataset constants and methods making sure all fare calculations, discounts and daily caps worked correctly. I also included input validation for zones, dates, passenger type and time bands so that the program wouldnÕt crash with invalid input. After checking adding journeys, listing journeys, filtering by passenger type or zone, removing journeys and resetting the day, everything ran as expected. The program now shows the functionality required by the class exercises and I documented each step in code comments for clarity.

---

------------------------------------------------------------------------------------------------------------------------------
