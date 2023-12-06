package org.WishCloud.Cloud;

import java.util.List;

public class Cloud {

    public static void main(String[] args) {
        // Define seeds for the ring
        List<String> seeds = List.of("localhost:8000", "localhost:8001");

        // Instantiate and start three servers
        startServer("localhost", 8000, seeds);
        startServer("localhost", 8001, seeds);
        startServer("localhost", 8002, seeds);
    }

    private static void startServer(String serverIp, int serverPort, List<String> seeds) {
        // Create and start a server
        Server server = new Server(serverIp, serverPort, seeds);
        server.start();
    }
}
