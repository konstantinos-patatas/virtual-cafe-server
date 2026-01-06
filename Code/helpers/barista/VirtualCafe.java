/**========================
    inspired by lab3 Bank class

    Entity class that represents the virtualCafe,
    holding the current state of the cafe's customers and orders at the moment

    what it does?
        * Manages the three areas (waiting, brewing, tray)
        * Tracks connected clients and their states
        * Handles order placement, brewing simulation, and collection
        * Provides thread-safe operations(startBrewingThread and synchronized methods)
        * Knows capacity constraints (2 teas, 2 coffees brewing max)
        * Logs state changes to both terminal and JSON file with timestamps

    why synchronized methods?
        * Uses coarse-grained synchronization (synchronized methods) for thread safety,
        * since Virtual Cafe is used by meany threads and sharing same space,
        * using synchronized we avoid race conditions

    Data Structure options for areas and why:
        Note: OrderItem class help as record for holding together an order item
              and its user, so one order might have many items but one customer
        * Waiting Area: Queue for first in first out implementation.
        * Brewing Area: Array list for fast efficient iteration and access,resize
        * Tray Area   : Holding customers and list for each that forms the order
                        of that customer, Using list so that items can be transferred later.
=========================**/

package helpers.barista;
import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VirtualCafe {
    // THREE EXPLICIT AREAS
    private final Queue<OrderItem> waitingArea = new LinkedList<>(); //FIFO
    private final List<OrderItem> brewingArea = new ArrayList<>();
    private final Map<String, List<OrderItem>> trayArea = new TreeMap<>();

    private final Map<String, Order> customers = new TreeMap<>();

    // Brewing capacity tracking
    private int currentBrewingTeas = 0;
    private int currentBrewingCoffees = 0;
    private static final int MAX_BREWING_TEAS = 2;
    private static final int MAX_BREWING_COFFEES = 2;

    // JSON logging
    private static final String LOG_FILE = "VritualCafe_logs.json";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public synchronized void customerConnected(String customerName) {
        if (!customers.containsKey(customerName)) {
            customers.put(customerName, new Order(customerName));
        }
        logState();
    }

    public synchronized void customerDisconnected(String customerName) {
        Order order = customers.remove(customerName);

        // Remove all items for this customer from all areas
        waitingArea.removeIf(item -> item.getCustomerName().equals(customerName));
        brewingArea.removeIf(item -> item.getCustomerName().equals(customerName));
        trayArea.remove(customerName);

        logState();
    }

    //bind the writer of the server created by the socket that communicates with client to the customer order for communication
    public synchronized void setClientWriter(String customerName, PrintWriter writer) {
        Order order = customers.get(customerName);
        if (order != null) {
            order.setClientWriter(writer);
        }
    }

    public synchronized void placeOrder(String customerName, int numTeas, int numCoffees)
            throws Exception {
        Order order = customers.get(customerName);
        if (order == null) {
            throw new Exception("Customer not found: " + customerName);
        }

        if (numTeas < 0 || numCoffees < 0) {
            throw new Exception("Order quantities must be non-negative.");
        }

        // 1. Update Order counters (for fast status lookup)
        order.setTeasWaiting(order.getTeasWaiting() + numTeas);
        order.setCoffeesWaiting(order.getCoffeesWaiting() + numCoffees);

        // 2. Add items to global waiting area (explicit data structure)
        for (int i = 0; i < numTeas; i++) {
            waitingArea.add(new OrderItem(customerName, helpers.barista.OrderItem.ItemType.TEA));
        }
        for (int i = 0; i < numCoffees; i++) {
            waitingArea.add(new OrderItem(customerName, helpers.barista.OrderItem.ItemType.COFFEE));
        }

        logState();
        startBrewingIfCapacityAvailable();
    }

    public synchronized String getOrderStatus(String customerName) throws Exception {
        Order order = customers.get(customerName);
        if (order == null) {
            throw new Exception("Customer not found: " + customerName);
        }

        if (order.isIdle()) {
            return "No order found for " + customerName;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Order status for ").append(customerName).append(":\n");

        if (order.getTeasWaiting() > 0 || order.getCoffeesWaiting() > 0) {
            sb.append("  - ").append(order.getCoffeesWaiting())
                    .append(" coffee and ").append(order.getTeasWaiting())
                    .append(" teas in waiting area\n");
        }

        if (order.getTeasBrewing() > 0 || order.getCoffeesBrewing() > 0) {
            sb.append("  - ").append(order.getCoffeesBrewing())
                    .append(" coffee and ").append(order.getTeasBrewing())
                    .append(" tea currently being prepared\n");
        }

        if (order.getTeasReady() > 0 || order.getCoffeesReady() > 0) {
            sb.append("  - ").append(order.getCoffeesReady())
                    .append(" coffees and ").append(order.getTeasReady())
                    .append(" teas currently in the tray\n");
        }

        return sb.toString();
    }

    // Check if order complete - O(1) using Order counters!
    public synchronized boolean isOrderComplete(String customerName) throws Exception {
        Order order = customers.get(customerName);
        if (order == null) {
            throw new Exception("Customer not found: " + customerName);
        }
        return order.isComplete();
    }

    // Collect order
    public synchronized String collectOrder(String customerName) throws Exception {
        Order order = customers.get(customerName);
        if (order == null) {
            throw new Exception("Customer not found: " + customerName);
        }

        if(order.isIdle()) {
            return "No order found for " + customerName;
        }

        if (!order.isComplete()) {
            throw new Exception("Order is still pending");
        }

        int teas = order.getTeasReady();
        int coffees = order.getCoffeesReady();

        // 1. Update Order counters
        order.setTeasReady(0);
        order.setCoffeesReady(0);

        // 2. Remove from global tray area
        trayArea.remove(customerName);

        logState();

        return "Order collected for " + customerName +
                " (" + teas + " teas and " + coffees + " coffees)";
    }

    public synchronized int getNumberOfClients() {
        return customers.size();
    }

    public synchronized int getNumberOfClientsWaiting() {
        int count = 0;
        for (Order order : customers.values()) {
            if (!order.isIdle()) {
                count++;
            }
        }
        return count;
    }

    // Start brewing if capacity available - stops when no items are waiting to be processed
    // since it is called again on startBrewingThread on finishBrewing and with a fresh iterator we get items skipped cause of capacity
    private synchronized void startBrewingIfCapacityAvailable() {
        Iterator<OrderItem> iterator = waitingArea.iterator();

        while (iterator.hasNext()) {
            OrderItem item = iterator.next();
            String customerName = item.getCustomerName();
            Order order = customers.get(customerName);

            if (item.getType() == helpers.barista.OrderItem.ItemType.TEA && currentBrewingTeas < MAX_BREWING_TEAS) {
                // 1. Update Order counters
                order.setTeasWaiting(order.getTeasWaiting() - 1);
                order.setTeasBrewing(order.getTeasBrewing() + 1);

                // 2. Move in global areas
                iterator.remove();  // From waiting
                brewingArea.add(item);  // To brewing

                currentBrewingTeas++;
                logState();
                startBrewingThread(item);

            } else if (item.getType() == helpers.barista.OrderItem.ItemType.COFFEE && currentBrewingCoffees < MAX_BREWING_COFFEES) {
                // 1. Update Order counters
                order.setCoffeesWaiting(order.getCoffeesWaiting() - 1);
                order.setCoffeesBrewing(order.getCoffeesBrewing() + 1);

                // 2. Move in global areas
                iterator.remove();  // From waiting
                brewingArea.add(item);  // To brewing

                currentBrewingCoffees++;
                logState();
                startBrewingThread(item);
            }
        }
    }

    // Start brewing thread for one item
    private void startBrewingThread(final OrderItem item) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(item.getBrewTimeMs());
                    finishBrewing(item);
                } catch (InterruptedException e) {
                    // Brewing interrupted
                }
            }
        }).start();
    }

    // Called when brewing completes
    private synchronized void finishBrewing(OrderItem item) {
        String customerName = item.getCustomerName();
        Order order = customers.get(customerName);

        if (order == null) return;  // Customer disconnected

        // 1. Update Order counters
        if (item.getType() == helpers.barista.OrderItem.ItemType.TEA) {
            order.setTeasBrewing(order.getTeasBrewing() - 1);
            order.setTeasReady(order.getTeasReady() + 1);
            currentBrewingTeas--;
        } else {
            order.setCoffeesBrewing(order.getCoffeesBrewing() - 1);
            order.setCoffeesReady(order.getCoffeesReady() + 1);
            currentBrewingCoffees--;
        }

        // 2. Move in global areas
        brewingArea.remove(item);  // From brewing
        trayArea.computeIfAbsent(customerName, k -> new ArrayList<>()).add(item);  // To tray

        logState();
        checkOrderCompletion(order); //notify user when its order is complete so to collect and the tray to be freed from the order
        startBrewingIfCapacityAvailable(); //call again to check remaining waiting items
    }

    // Notify customer if order complete
    private void checkOrderCompletion(Order order) {
        if (order.isComplete()) {
            PrintWriter writer = order.getClientWriter();
            if (writer != null) {
                int teas = order.getTeasReady();
                int coffees = order.getCoffeesReady();
                writer.println("\nOrder completed for " + order.getCustomerName() +
                        " (" + teas + " teas and " + coffees + " coffees). Please collect!");
            }
        }
    }

    // Enhanced logging with event type and customer name
    private void logState() {
        // Console logging
        System.out.println("\n=== Cafe State ===");
        System.out.println("Clients in café: " + getNumberOfClients());
        System.out.println("Clients waiting for orders: " + getNumberOfClientsWaiting());

        // Count items by type - O(m) but acceptable for logging
        int teasWaiting = 0, coffeesWaiting = 0;
        int teasBrewing = 0, coffeesBrewing = 0;
        int teasReady = 0, coffeesReady = 0;

        for (OrderItem item : waitingArea) {
            if (item.getType() == helpers.barista.OrderItem.ItemType.TEA) teasWaiting++;
            else coffeesWaiting++;
        }

        for (OrderItem item : brewingArea) {
            if (item.getType() == helpers.barista.OrderItem.ItemType.TEA) teasBrewing++;
            else coffeesBrewing++;
        }

        for (List<OrderItem> items : trayArea.values()) {
            for (OrderItem item : items) {
                if (item.getType() == helpers.barista.OrderItem.ItemType.TEA) teasReady++;
                else coffeesReady++;
            }
        }

        System.out.println("Waiting area: " + teasWaiting + " teas, " + coffeesWaiting + " coffees");
        System.out.println("Brewing area: " + teasBrewing + " teas, " + coffeesBrewing + " coffees");
        System.out.println("Tray area: " + teasReady + " teas, " + coffeesReady + " coffees");
        System.out.println("==================\n");

        // JSON logging
        writeJsonLog(teasWaiting, coffeesWaiting,
                teasBrewing, coffeesBrewing, teasReady, coffeesReady);
    }

    // Write JSON log entry to file
    private void writeJsonLog(int teasWaiting, int coffeesWaiting, int teasBrewing,
                              int coffeesBrewing, int teasReady, int coffeesReady) {
        try {
            File logFile = new File(LOG_FILE);
            boolean isNewFile = !logFile.exists() || logFile.length() == 0;

            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

            // Build JSON entry
            StringBuilder json = new StringBuilder();

            if (isNewFile) {
                // Start of file - open array
                json.append("[\n");
            } else {
                // Existing file - need to remove closing bracket and add comma
                removeLastClosingBracket();
                json.append(",\n");
            }

            // Add the new JSON object
            json.append("  {\n");
            json.append("    \"timestamp\": \"").append(timestamp).append("\",\n");
            json.append("    \"totalClients\": ").append(getNumberOfClients()).append(",\n");
            json.append("    \"clientsWithOrders\": ").append(getNumberOfClientsWaiting()).append(",\n");
            json.append("    \"waitingArea\": {\n");
            json.append("      \"teas\": ").append(teasWaiting).append(",\n");
            json.append("      \"coffees\": ").append(coffeesWaiting).append("\n");
            json.append("    },\n");
            json.append("    \"brewingArea\": {\n");
            json.append("      \"teas\": ").append(teasBrewing).append(",\n");
            json.append("      \"coffees\": ").append(coffeesBrewing).append("\n");
            json.append("    },\n");
            json.append("    \"trayArea\": {\n");
            json.append("      \"teas\": ").append(teasReady).append(",\n");
            json.append("      \"coffees\": ").append(coffeesReady).append("\n");
            json.append("    }\n");
            json.append("  }\n");
            json.append("]");  // Close array (will be removed next time if more entries added)

            // Write to file
            try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
                fw.write(json.toString());
            }

        } catch (IOException e) {
            System.err.println("Failed to write JSON log: " + e.getMessage());
        }
    }

    // Helper method to remove the last closing bracket from the file to each time create valid json syntax
    private void removeLastClosingBracket() throws IOException {
        File logFile = new File(LOG_FILE);

        // Read entire file
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        // Remove trailing ']' and whitespace
        String fileContent = content.toString().trim();
        if (fileContent.endsWith("]")) {
            fileContent = fileContent.substring(0, fileContent.length() - 1).trim();
        }

        // Write back without the closing bracket
        try (FileWriter writer = new FileWriter(logFile, false)) {
            writer.write(fileContent);
        }
    }
}