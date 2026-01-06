/**=================
    what it does?
        Class that handles what the server communicates with the client
        Important since it separates concerns on the server

    Inspired by lab3 ClientHandler class

    args:
        Server/barista Socket that forms bidirectional communication with client
===================**/

package helpers.barista;
import java.io.PrintWriter;  import java.util.Scanner;
import java.net.Socket;



public class CustomerHandler implements Runnable {
    private final Socket socket; //socket that communicates with the server
    private final VirtualCafe virtualCafe;
    String customerName = null;

    public CustomerHandler(Socket socket, VirtualCafe virtualCafe) {
        this.socket = socket;
        this.virtualCafe = virtualCafe;
    }

    @Override
    public void run() {
        try( Scanner scanner = new Scanner(socket.getInputStream());
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            try{
                //ask the client/customer for its name
                writer.println("Welcome Please enter your name: ");
                customerName = scanner.nextLine().trim(); //get answer

                if (customerName.isEmpty()) {
                    throw new Exception("Name cannot be empty.");
                }
                virtualCafe.customerConnected(customerName);
                virtualCafe.setClientWriter(customerName, writer); //Set client writer for completion notifications
                writer.println( "Hello " + customerName + "!" );
                System.out.println(customerName + " has entered VirtualCafe.");

                //listen for incoming commands
                while (true) {
                    String line = scanner.nextLine().trim();

                    if (line.isEmpty()) {
                        continue;
                    }

                    // Parse command (case-insensitive)
                    String lowerLine = line.toLowerCase();
                    String[] words = lowerLine.split("\\s+");

                    if (words.length == 0) {
                        continue;
                    }

                    // Check multi-word commands first
                    if (lowerLine.startsWith("order status")) {
                        try {
                            String status = virtualCafe.getOrderStatus(customerName);
                            writer.println(status);
                        } catch (Exception e) {
                            writer.println("ERROR " + e.getMessage());
                        }
                    } else if (lowerLine.startsWith("order")) {
                        try {
                            OrderResult orderResult = parseOrderCommand(words);

                            // Place order through Cafe
                            virtualCafe.placeOrder(customerName, orderResult.numTeas, orderResult.numCoffees);

                            // Acknowledge order by name
                            writer.println("Order received for " + customerName +
                                    " (" + orderResult.numTeas + " teas and " +
                                    orderResult.numCoffees + " coffees)");

                        } catch (NumberFormatException e) {
                            writer.println("ERROR Invalid number format in order.");
                        } catch (Exception e) {
                            writer.println("ERROR " + e.getMessage());
                        }
                    } else if (lowerLine.equals("collect")) {
                        try {
                            String result = virtualCafe.collectOrder(customerName);
                            writer.println(result);
                        } catch (Exception e) {
                            writer.println("ERROR " + e.getMessage());
                        }
                    } else if (lowerLine.equals("exit")) {
                        virtualCafe.customerDisconnected(customerName);
                        writer.println("Goodbye " + customerName);
                        socket.close();
                        return;
                    } else {
                        writer.println("ERROR Unknown command: " + lowerLine +
                                ". Valid commands: order, order status, collect, exit");
                    }
                }

            }catch (Exception e) {
                writer.println("ERROR " + e.getMessage());
                socket.close();
            }


        }catch(Exception e) {
            // Socket closing exception - silent catch
            System.err.println("Connection error for customer " +
                    (customerName != null ? customerName : "unknown") +
                    ": " + e.getMessage());
        }finally {
            if (customerName != null) {
                virtualCafe.customerDisconnected(customerName);
                System.out.println(customerName + " has Left Virtual Cafe.");
            }
        }
    }

    //=====helpers for command parsing=====
    /**
     * Parse order command to extract number of teas and coffees.

     * @param words Command split into words (already lowercase)
     * @return OrderResult with numTeas and numCoffees
     * @throws Exception if format is invalid
     */
    private OrderResult parseOrderCommand(String[] words) throws Exception {
        int numTeas = 0;
        int numCoffees = 0;

        // Need at least 3 words: "order", quantity, item
        if (words.length < 3) {
            throw new Exception("Invalid order format. Use: order <quantity> <tea/coffee> [and <quantity> <tea/coffee>]");
        }

        int i = 1;  // Start after "order"

        while (i < words.length) {
            // Expect a number
            if (i >= words.length) {
                throw new Exception("Expected quantity after 'order' or 'and'");
            }

            int quantity;
            try {
                quantity = Integer.parseInt(words[i]);
                i++;
            } catch (NumberFormatException e) {
                throw new Exception("Expected a number, got: " + words[i]);
            }

            if (quantity <= 0) {
                throw new Exception("Quantity must be positive");
            }

            // Expect tea/teas or coffee/coffees
            if (i >= words.length) {
                throw new Exception("Expected 'tea' or 'coffee' after quantity");
            }

            String item = words[i];
            i++;

            // Match tea/teas or coffee/coffees
            if (item.equals("tea") || item.equals("teas")) {
                numTeas += quantity;
            } else if (item.equals("coffee") || item.equals("coffees")) {
                numCoffees += quantity;
            } else {
                throw new Exception("Unknown item: " + item + ". Use 'tea' or 'coffee'");
            }

            // Check if there's an "and" for another item
            if (i < words.length) {
                if (words[i].equals("and")) {
                    i++;  // Skip "and"

                    if (i >= words.length) {
                        throw new Exception("Expected quantity after 'and'");
                    }
                } else {
                    throw new Exception("Unexpected word: " + words[i] + ". Expected 'and' or end of command");
                }
            }
        }

        // Ensure at least one item was ordered
        if (numTeas == 0 && numCoffees == 0) {
            throw new Exception("No items ordered");
        }

        return new OrderResult(numTeas, numCoffees);
    }

    /**
     * Helper class to return parsed order results
     */
    private static class OrderResult {
        final int numTeas;
        final int numCoffees;

        OrderResult(int numTeas, int numCoffees) {
            this.numTeas = numTeas;
            this.numCoffees = numCoffees;
        }
    }
}











