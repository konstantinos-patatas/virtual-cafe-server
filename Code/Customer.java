/**====================================================================================
 Customer client for the Virtual Café.

 * Similar to ClientProgram.java in the bank example, but with main method
 * and client architecture for socket communication similar to Client.java bank example

 Connects to the Barista server and allows users to:
 * Order tea and coffee
 * Check order status
 * Collect completed orders
 * Exit the café

 * Key differences from bank example:
 * Uses background thread to handle async server notifications
 (server notifies when order is complete)
 ====================================================================================**/
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Customer {
    private static final int PORT = 8888;
    private static final String HOST = "localhost";

    private static final String NAVIGATOR = "V-Cafe>";

    private static void displayCommands() {
        System.out.println(" ");
        System.out.println("Available commands:");
        System.out.println(" * order <quantity> <tea/coffee> [and <quantity> <tea/coffee>]");
        System.out.println("       Examples:");
        System.out.println("         order 1 tea");
        System.out.println("         order 2 coffees");
        System.out.println("         order 2 teas and 3 coffees");
        System.out.println("         order 1 coffee and 4 teas \n");
        System.out.println(" * order status    - Check your order status");
        System.out.println(" * collect         - Collect your completed order");
        System.out.println(" * exit            - Leave the café");
        System.out.println("===============================================================");
    }

    public static void main(String[] args) {
        System.out.println("=== Welcome to Virtual Café  ===\n");

        final Scanner fromUser = new Scanner(System.in);

        // flag to determine normal to emergency(ctrl-c) exit
        AtomicBoolean normalExit = new AtomicBoolean(false);

        // Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!normalExit.get()) {
                // Use System.err or a delay to ensure the message prints
                // before the terminal process is cleared.
                System.err.println("\n *** SIGINT received (Ctrl-C or closed from IDE). Emergency exit triggered. ***");
                System.out.println("\nThank you for visiting Virtual Café!");

                // Add a small delay for output stability
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }));

        try (Socket socket = new Socket(HOST, PORT);
             Scanner fromServer = new Scanner(socket.getInputStream());
             PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
        ) {

            String welcomeMessage = fromServer.nextLine(); //server welcomes and asks for name
            System.out.print(welcomeMessage);

            String customerName = fromUser.nextLine().trim();
            toServer.println(customerName);

            String serverResponse = fromServer.nextLine();
            System.out.println(serverResponse);

            // If server sends error, exit
            if (serverResponse.startsWith("ERROR")) {
                return;
            }

            // sends ASYNC notifications when order is complete.
            // Background thread continuously reads and prints server messages.
            Thread messageListener = new Thread(() -> {
                try {
                    while (fromServer.hasNextLine()) {
                        String message = fromServer.nextLine();
                        System.out.println(message);
                    }
                } catch (Exception e) {
                    // Connection closed - normal when exiting
                }
            });
            messageListener.setDaemon(true);  // Daemon thread exits when main exits
            messageListener.start();

            displayCommands();

            // Main Command Loop for running commands and getting responses
            while (true) {
                System.out.print("\n" + NAVIGATOR + " ");
                String command = fromUser.nextLine().trim();

                if (command.isEmpty()) {
                    continue;
                }

                toServer.println(command);

                // Brief pause to allow server response to be printed by listener thread
                // before showing next prompt (makes output cleaner)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore interruption
                }

                if (command.toLowerCase().equals("exit")) {
                    normalExit.set(true); // Tell the hook this was a planned exit
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Error connecting to Virtual Café: " + e.getMessage());
            System.err.println("Please ensure the Barista server is running on " + HOST + ":" + PORT);
        }

        System.out.println("\nThank you for visiting Virtual Café!");
    }
}