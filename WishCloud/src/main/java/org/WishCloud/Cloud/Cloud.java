package org.WishCloud.Cloud;

import java.util.List;

public class Cloud {

    public static void main(String[] args) {
        // Define seeds for the ring
        List<String> seeds = List.of("localhost:8000", "localhost:8001", "localhost:8002");

        // Loop through the seeds and start a server for each one
        for (String seed : seeds) {
            String[] serverInfo = seed.split(":");
            String serverIp = serverInfo[0];
            int serverPort = Integer.parseInt(serverInfo[1]);
            startServer(serverIp, serverPort, seeds);
        }

    }

    private static void startServer(String serverIp, int serverPort, List<String> seeds) {
        // Create and start a server
        Server server = new Server(serverIp, serverPort, seeds);
        server.start();
    }
}
