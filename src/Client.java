import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private final Scanner scanner;

    public Client() {
        scanner = new Scanner(System.in);
    }

    private boolean isValidIP(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }

    private boolean isValidPort(int port) {
        return port >= 5000 && port <= 5050;
    }

    public void connect() {
        System.out.print("Entrez l'adresse IP du serveur: ");
        String serverIP = scanner.nextLine();
        while (!isValidIP(serverIP)) {
            System.out.print("L'adresse IP du serveur est invalide! Réessayez : ");
            serverIP = scanner.nextLine();
        }

        System.out.print("Entrez le port du serveur (5000-5050): ");
        while (!scanner.hasNextInt()) {
            System.out.print("Le port du serveur est invalide. Entrez une valeur entre 5000 et 5050: ");
            scanner.next();
        }
        int serverPort = scanner.nextInt();
        while (!isValidPort(serverPort)) {
            System.out.print("Port de serveur invalide. Entrez une valeur entre 5000 et 5050: ");
            serverPort = scanner.nextInt();
        }
        scanner.nextLine(); // Nettoyer le buffer

        try {
            socket = new Socket(serverIP, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            System.out.print("Entrez votre nom d'utilisateur: ");
            String username = scanner.nextLine();
            System.out.print("Entrez votre mot de passe: ");
            String password = scanner.nextLine();

            output.println(username + "," + password);

            String response = input.readLine();
            if ("ERROR".equals(response)) {
                System.out.println("Erreur dans la saisie du mot de passe.");
                closeConnection();
                return;
            }
            System.out.println("Connexion réussie. Vous pouvez maintenant clavarder!");

            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

            sendMessages();
        } catch (IOException e) {
            System.out.println("Erreur de connexion au serveur: " + e.getMessage());
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = input.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Déconnecté du serveur.");
        }
    }

    private void sendMessages() {
        try {
            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("/exit")) {
                    closeConnection();
                    break;
                }
                if (message.length() > 200) {
                    System.out.println("Message trop long (max 200 caractères).");
                    continue;
                }
                output.println(message);
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de l'envoi du message.");
        }
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();
            System.out.println("Déconnexion réussie.");
        } catch (IOException e) {
            System.out.println("Erreur lors de la fermeture de connexion.");
        }
    }

    public static void main(String[] args) {
        new Client().connect();
    }
}
