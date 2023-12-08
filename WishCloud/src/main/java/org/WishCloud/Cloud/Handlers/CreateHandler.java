package org.WishCloud.Cloud.Handlers;

import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import java.net.http.HttpClient;
import com.sun.net.httpserver.HttpExchange;

public class CreateHandler extends ServerHandler {
    public CreateHandler(String serverName, Ring ring, SQl db) {
        super(serverName, ring, db);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get attributes from the exchange
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String,String> params = queryToMap(exchange.getRequestURI().getQuery());
        byte[] body = exchange.getRequestBody().readAllBytes();

        if (!method.equals("POST") || !path.equals("/create")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }


        // check if the shopping list is null or already exists in database
        ShoppingList shoppingList;
        if (getDb().getShoppingList(params.get("uuid")) != null) {
            sendResponse(exchange, 400, "Shopping list already exists!");
            return;
        } else if ((shoppingList = Serializer.deserialize(body)) == null) {
            sendResponse(exchange, 400, "Shopping list corrupted!");
            return;
        } else if (getDb().insertSL(shoppingList)) {
            // when implementing hinted handoff, this will be changed to insertSL(shoppingList, params.get("hinted")))
            sendResponse(exchange, 500, "Error loading shopping list on database!");
            return;
        }

        if (!params.get("cord").equals("true")) {
            sendResponse(exchange, 200, "Shopping list created!");
            return;
        }

        List<String> orderedNodes = getRing().getPreferenceList(params.get("uuid"));
        List<String> preferenceList = getRing().getPreferenceList(params.get("uuid"), this.replicas);
        Set<String> servedNodes = new HashSet<>();
        Set<String> hintedNodes = new HashSet<>();
        servedNodes.add(getServerName());
        for (String server : orderedNodes.subList(orderedNodes.indexOf(getServerName())+1, orderedNodes.size())) {
            // Check if the replica creation was successful
            if (servedNodes.size() == this.replicas) { break; }

            String hintedNode = null;
            StringBuilder url = new StringBuilder("http://" + server + "/create?uuid=" + params.get("uuid") + "&cord=false");
            if (!preferenceList.contains(server)) {
                // find server in the preference list that was not served yet
                 hintedNode = preferenceList.stream()
                        .filter(node -> !servedNodes.contains(node) && !hintedNodes.contains(node))
                        .findFirst().orElse(null);
                url.append("&hinted=").append(hintedNode);
            }

            // send the shopping list to the next server in the ring
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    System.out.println("\nReplica of list "+ params.get("uuid") + " in " + server + " created! Server Response: " + response.body());
                    servedNodes.add(server);
                    if (hintedNode != null) { hintedNodes.add(hintedNode); }
                } else {
                    System.out.println("\nError creating replica of list " + params.get("uuid") + " in " + server + "! Server Response: " + response.body());
                }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        if (servedNodes.size() != this.replicas) {
            sendResponse(exchange, 500, "Error creating replicas!");
            // undo the creation of the shopping list both in the database and in the replicas
            return;
        }

        sendResponse(exchange, 200, "Shopping list created!");
    }
}
