/**=======================================
    Our server application called barista,handling :
    1)listening and accepting for clients(customers) with server socket
    2) a run static method to call on the main function that will start the server
 ========================================**/
import java.io.IOException; import java.net.ServerSocket; import java.net.Socket;
import helpers.barista.VirtualCafe; import helpers.barista.CustomerHandler;


public class Barista {
    private final static int port = 8888; //after 8000 port number is easier to avoid conflicts
    private static final VirtualCafe virtualCafe = new VirtualCafe();

    public static void main(String[] args) {
        startShift();
    }

    private static void startShift(){
        ServerSocket serverSocket = null;

        try{
            serverSocket = new ServerSocket(port); //server listening in specified port
            System.out.println("✔ Virtual Cafe Server Started");
            System.out.println("Barista is waiting for customers to join the Virtual cafe..." );

            //accepting many users to enter the cafe
            while(true){
                Socket socket = serverSocket.accept();
                new Thread(new CustomerHandler(socket, virtualCafe)).start();
            }

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
