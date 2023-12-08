package org.WishCloud.Cloud;

import java.util.ArrayList;
import java.util.List;

public class Cloud {

    public static void main(String[] args) {
        // Define seeds for the ring
        int numberOfSeeds; // Change this to the desired number of seeds
        if (args.length > 0) {
            numberOfSeeds = Integer.parseInt(args[0]);
        } else {
            numberOfSeeds = 3;
        }
        List<String> seeds = new ArrayList<>();

        for (int i = 0; i < numberOfSeeds; i++) {
            seeds.add("localhost:80" + (i < 10 ? "0" + i : i));
        }

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
