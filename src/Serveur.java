import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class Serveur {
    private static final int PORT = 5000;
    private static final Map<String, String> userDatabase = new HashMap<>(); // username -> password
    private static final List<String> messageHistory = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur en écoute sur le port " + PORT);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String credentials = in.readLine();
                String[] parts = credentials.split(",");
                String username = parts[0];
                String password = parts[1];

                if (userDatabase.containsKey(username) && !userDatabase.get(username).equals(password)) {
                    out.println("ERROR");
                    return;
                } else {
                    userDatabase.put(username, password);
                    out.println("SUCCESS");
                }

                messageHistory.forEach(out::println);
                String message;
                while ((message = in.readLine()) != null) {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss").format(new Date());
                    String formattedMessage = String.format("[%s - %s:%d - %s]: %s", username, socket.getInetAddress(), socket.getPort(), timestamp, message);
                    System.out.println(formattedMessage);
                    messageHistory.add(formattedMessage);
                }
            } catch (IOException e) {
                System.err.println("Erreur avec le client: " + e.getMessage());
            }
        }
    }
}
