package org.WishCloud.Cloud;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ServerManager {

    public static void main(String[] args) {

        // get arguments from the command line
        String action = args[0];
        String host = args[1];
        String port = args[2];
        int numberOfSeeds = Integer.parseInt(args[3]);

        // Create a list of seeds
        List<String> seeds = new ArrayList<>();

        // Add the seeds to the list
        for (int i = 0; i < numberOfSeeds; i++) {
            seeds.add("localhost:80" + (i < 10 ? "0" + i : i));
        }

        switch (action) {
            case "start":
                startServer(host, Integer.parseInt(port), seeds);
                break;
            case "stop":
                stopServer(host, port);
                break;
            case "check":
                checkServer(host, port);
                break;
            default:
                System.out.println("Invalid action!");
                break;
        }


    }

    private static void checkServer(String host, String port) {
        // Create a socket
        Socket socket = new Socket();

        try {
            // Connect to the server
            socket.connect(new InetSocketAddress(host, Integer.parseInt(port)), 1000);
            System.out.println("Server is running!");
        } catch (IOException e) {
            System.out.println("Server is not running!");
        }
    }

    private static void startServer(String serverIp, int serverPort, List<String> seeds) {
        // Create and start a server
        Server server = new Server(serverIp, serverPort, seeds);
        server.start();
    }

    private static void stopServer(String host, String port) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + host + ":" + port + "/kill?uuid=1&cord=true"))
                .GET()
                .build();
        try {
            // Send the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (InterruptedException | IOException e) {
            // Print the full stack trace for better debugging
            e.printStackTrace();
        }
    }
}