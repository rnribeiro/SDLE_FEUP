package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.net.URI;
import java.net.http.HttpRequest;
import org.WishCloud.CRDT.CRDT;
import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Serializer;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.QueryParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReadHandler implements HttpHandler {
    private final String serverName;
    private final Ring ring;
    private final SQl db;

    public ReadHandler(String serverName, Ring ring, SQl db) {
        this.serverName = serverName;
        this.ring = ring;
        this.db = db;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            // Parse query parameters
            Map<String, String> params = QueryParser.queryToMap(exchange.getRequestURI().getQuery());

            // Check if required parameters are present
            if (!params.containsKey("uuid") || !params.containsKey("cord")) {
                sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing required parameters");
                return;
            }

            String listId = params.get("uuid");
            boolean isCord = Boolean.parseBoolean(params.get("cord"));

            // Check if the list exists in the local server database
            if (db.getShoppingList(listId) == null) {
                sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "List does not exist");
                return;
            }

            if (!isCord) {
                // If not a CORD request, return the local list serialized
                ShoppingList localList = db.getShoppingList(listId);
                String serializedList = Arrays.toString(Serializer.serialize(localList));
                sendResponse(exchange, HttpURLConnection.HTTP_OK, serializedList);
            } else {
                // If CORD request, compute the preference list and read from other servers
                List<String> preferenceList = ring.getPreferenceList(listId, 3);
                ShoppingList mergedList = null;

                for (String server : preferenceList) {
                    if (!server.equals(serverName)) {
                        // Send a read request to the next server in the preference list
                        ShoppingList remoteList = readFromRemoteServer(server, listId);
                        if (remoteList != null) {
                            // Merge the remote list with the local list
                            mergedList = mergedList != null ? mergedList.merge(remoteList.getListItems()) : remoteList;
                        }
                    }
                }

                if (mergedList != null) {
                    // If all went well, return the merged list to the client
                    String serializedMergedList = Arrays.toString(Serializer.serialize(mergedList));
                    sendResponse(exchange, HttpURLConnection.HTTP_OK, serializedMergedList);
                } else {
                    // If something went wrong, return an error response
                    sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error");
                }
            }
        }
    }

    private ShoppingList readFromRemoteServer(String remoteServer, String listId) {
        // Construct the URL for the remote server
        String remoteUrl = "http://" + remoteServer + "/read?uuid=" + listId + "&cord=false";

        try {
            // Send a GET request to the remote server
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(remoteUrl))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                // Deserialize the received shopping list
                return Serializer.deserialize(response.body().getBytes());
            } else {
                // Log an error if the request was not successful
                System.out.println("Error reading from remote server: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            // Log an error if an exception occurred during the request
            e.printStackTrace();
        }

        return null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
