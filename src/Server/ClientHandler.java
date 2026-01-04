package Server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ServerMain server;
    private PrintWriter out;
    private String username; //client user

    public ClientHandler(Socket s, ServerMain srv) {
        socket = s;
        server = srv;
    }

    public void run() {
        //main
        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            out = new PrintWriter(socket.getOutputStream(), true);

            String join = in.readLine();
            if (join == null || !join.startsWith("JOIN|")) {
                socket.close();
                return;
            }

            username = join.substring(5).trim();
            if (username.isEmpty()) {
                username = "IHaveNoName" + System.currentTimeMillis();
            }

            if (!server.addClient(username, this)) {
                return;
            }

            String msg;
            while ((msg = in.readLine()) != null) {
                server.broadcast(msg, this);
            }
        } catch (Exception e) {} finally {
            server.removeClient(username);
            try {
                socket.close();
            } catch (Exception e) {}
        }
    }

    public void send(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush();
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {}
    }

    public String getUsername() {
        return username;
    }
}
