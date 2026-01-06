/**==================
    inspired by lab3 Account

     Represents one customer's complete order in the café.
         * Similar to Account in Bank example - stores customer's order data.
         * Tracks counts of items in each area for fast O(1) lookups.
         * These counters are kept in sync with global areas in Cafe.java.
==================**/

package helpers.barista;
import java.io.PrintWriter;

public class Order {
    private final String customerName;
    private PrintWriter clientWriter;  // For sending responses

    // Three areas: waiting, brewing, tray
    private int teasWaiting;
    private int coffeesWaiting;
    private int teasBrewing;
    private int coffeesBrewing;
    private int teasReady;
    private int coffeesReady;

    public Order(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerName()    { return customerName; }
    public int getTeasWaiting()        { return teasWaiting; }
    public int getCoffeesWaiting()     { return coffeesWaiting; }
    public int getTeasBrewing()        { return teasBrewing; }
    public int getCoffeesBrewing()     { return coffeesBrewing; }
    public int getTeasReady()          { return teasReady; }
    public int getCoffeesReady()       { return coffeesReady; }
    public PrintWriter getClientWriter() { return clientWriter; }

    //package-protected (default) so taht only Virtualcafe class can modify
    void setTeasWaiting(int teasWaiting)       { this.teasWaiting = teasWaiting; }
    void setCoffeesWaiting(int coffeesWaiting) { this.coffeesWaiting = coffeesWaiting; }
    void setTeasBrewing(int teasBrewing)       { this.teasBrewing = teasBrewing; }
    void setCoffeesBrewing(int coffeesBrewing) { this.coffeesBrewing = coffeesBrewing; }
    void setTeasReady(int teasReady)           { this.teasReady = teasReady; }
    void setCoffeesReady(int coffeesReady)     { this.coffeesReady = coffeesReady; }
    void setClientWriter(PrintWriter writer)   { this.clientWriter = writer; }


    public boolean isIdle() {
        return (teasWaiting + coffeesWaiting + teasBrewing +
                coffeesBrewing + teasReady + coffeesReady) == 0;
    }

    public boolean isComplete() {
        return (teasWaiting + coffeesWaiting + teasBrewing + coffeesBrewing) == 0
                && (teasReady + coffeesReady) > 0;
    }

    public int getTotalItems() {
        return teasWaiting + coffeesWaiting + teasBrewing +
                coffeesBrewing + teasReady + coffeesReady;
    }
}
