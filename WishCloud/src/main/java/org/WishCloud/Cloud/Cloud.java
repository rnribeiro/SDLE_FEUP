package org.WishCloud.Cloud;

import java.util.ArrayList;
import java.util.List;

public class Cloud {

    public static void main(String[] args) {

        // Get the number of seeds from the command line arguments or use a default value
        int numberOfSeeds;
        if (args.length > 0) {
            numberOfSeeds = Integer.parseInt(args[0]);
        } else {
            numberOfSeeds = 3;
        }

        // Create a list of seeds
        List<String> seeds = new ArrayList<>();

        // Add the seeds to the list
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
