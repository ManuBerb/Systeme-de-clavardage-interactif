import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

public class Serveur {
    private static final int MIN_PORT = 5000;
    private static final int MAX_PORT = 5050;
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static final List<String> chatHistory = new ArrayList<>();
    private static final Set<PrintWriter> clientOutputs = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Entrez l'adresse IP du serveur: ");
        String serverIP = scanner.nextLine();
        while (!isValidIP(serverIP)) {
            System.out.print("Adresse IP invalide. Réessayez: ");
            serverIP = scanner.nextLine();
        }

        System.out.print("Entrez le port du serveur (5000-5050): ");
        int port = scanner.nextInt();
        while (port < MIN_PORT || port > MAX_PORT) {
            System.out.print("Port invalide. Réessayez: ");
            port = scanner.nextInt();
        }

        scanner.nextLine(); // Nettoyer le buffer

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(serverIP))) {
            System.out.println("Serveur démarré sur " + serverIP + ":" + port);

            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("Erreur lors du démarrage du serveur: " + e.getMessage());
        }
    }

    private static boolean isValidIP(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter output;
        private String username;
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                output.println("Entrez votre nom d'utilisateur et mot de passe (format: username,password)");
                String[] credentials = input.readLine().split(",");
                username = credentials[0];
                String password = credentials[1];

                if (!users.containsKey(username)) {
                    users.put(username, password);
                } else if (!users.get(username).equals(password)) {
                    output.println("ERROR");
                    socket.close();
                    return;
                }

                output.println("Connexion réussie.");
                clientOutputs.add(output);
                sendChatHistory(output);

                String message;
                while ((message = input.readLine()) != null) {
                    if (message.equalsIgnoreCase("/exit")) {
                        break;
                    }
                    String timestamp = dateFormat.format(new Date());
                    String formattedMessage = "[" + username + " - " + socket.getInetAddress() + ":" + socket.getPort() + " - " + timestamp + "] " + message;
                    System.out.println(formattedMessage);
                    chatHistory.add(formattedMessage);
                    broadcastMessage(formattedMessage);
                }
            } catch (IOException e) {
                System.out.println("Erreur avec le client: " + username);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Erreur lors de la fermeture de connexion.");
                }
                clientOutputs.remove(output);
            }
        }

        private void sendChatHistory(PrintWriter output) {
            for (String msg : chatHistory.subList(Math.max(chatHistory.size() - 15, 0), chatHistory.size())) {
                output.println(msg);
            }
        }

        private void broadcastMessage(String message) {
            for (PrintWriter writer : clientOutputs) {
                writer.println(message);
            }
        }
    }
}
