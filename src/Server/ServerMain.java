package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import javax.swing.*;

public class ServerMain {

    private Map<String, ClientHandler> clients = new HashMap<>();
    private List<String> history = new ArrayList<>();
    private AdminPanel admin;
    private Set<String> bannedUsers = new HashSet<>();

    public static void main(String[] args) {
        new ServerMain().start();
    }

    public ServerMain() {
        SwingUtilities.invokeLater(() -> {
            admin = new AdminPanel(this);
            admin.setVisible(true);
        });
    }

    //main server start
    public void start() {
        try (ServerSocket server = new ServerSocket(12345)) {
            System.out.println("Server hosted on port 12345");

            while (true) {
                Socket client = server.accept();
                new Thread(new ClientHandler(client, this)).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    //--

    //store the banned clients name
    public synchronized boolean addClient(
        String username,
        ClientHandler handler
    ) {
        if (bannedUsers.contains(username)) {
            handler.send("BANNED");
            handler.disconnect();
            if (admin != null) admin.log("banned user detected: " + username);
            return false; //checks and returns if anything sneaky ppl tryna join
        }

        clients.put(username, handler);
        if (admin != null) {
            admin.log("Joined: " + username);
            admin.updateUsers();
        }

        // Send history to new client
        for (String msg : history) {
            handler.send(msg);
        }

        return true;
    }

    //--

    //left logs
    public synchronized void removeClient(String username) {
        clients.remove(username);
        if (admin != null) {
            admin.log("Left: " + username);
            admin.updateUsers();
        }
    }

    public synchronized void broadcast(String msg, ClientHandler sender) {
        //cliens work
        if (msg.startsWith("CLEARMY|")) {
            String clientId = msg.substring(8); //id
            history.removeIf(h -> h.contains("|" + clientId + "|"));

            for (ClientHandler c : clients.values()) {
                c.send(msg);
            }
            return;
        }

        //cmd
        if (!msg.equals("CLEAR")) {
            history.add(msg);
        } else {
            history.clear();
        }

        for (ClientHandler c : clients.values()) {
            c.send(msg);
        }
    }

    //kicks
    public void kick(String username) {
        ClientHandler handler = clients.get(username);
        if (handler != null) {
            handler.send("KICKED");
            handler.disconnect();
            clients.remove(username);
        }
    }

    //ban system fixed
    public void ban(String username) {
        bannedUsers.add(username);

        ClientHandler handler = clients.get(username);
        if (handler != null) {
            handler.send("BANNED");
            handler.disconnect();
            clients.remove(username);
        }
    }

    public List<String> getUsers() {
        return new ArrayList<>(clients.keySet());
    }
}
