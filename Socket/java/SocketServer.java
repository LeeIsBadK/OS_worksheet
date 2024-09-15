// SocketServer.java
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SocketServer {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected with IP address: " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread for each client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (OutputStream output = clientSocket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            while (true) {
                // Get the current date and time
                String currentTime = formatter.format(new Date());

                // Send the current date and time to the client
                writer.println("Current date and time: " + currentTime);

                // Sleep for 1 second before sending the next update
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException ex) {
            System.out.println("Client disconnected: " + ex.getMessage());
        } finally {
            try {
                System.out.println("Closing the client socket at IP address: " + clientSocket.getInetAddress().getHostAddress());
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println("Failed to close client socket: " + ex.getMessage());
            }
        }
    }
}
