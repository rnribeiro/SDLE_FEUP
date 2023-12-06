package org.WishCloud.Cloud.Handlers;

import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

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
                || !params.containsKey("uuid") || !params.containsKey("author")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }


        // check if the shopping list is null or already exists in database
        ShoppingList shoppingList;
        if (getDb().getShoppingList(params.get("uuid")) == null) {
            sendResponse(exchange, 400, "Shopping list already exists!");
            return;
        } else if ((shoppingList = Serializer.deserialize(body)) == null) {
            sendResponse(exchange, 400, "Shopping list corrupted!");
            return;
        }

        int replicas = 2;
        List<String> preferenceList = getRing().getPreferenceList(params.get("uuid"), replicas);
        if (preferenceList.get(0).equals(getServerName())) {
            for (String server : preferenceList.subList(1, preferenceList.size())) {
                // send the shopping list to the next server in the ring
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/create?uuid=" + params.get("uuid") + "&author=" + getServerName()))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        System.out.println("\nReplica in " + server + " created! Server Response: " + response.body());
                        replicas--;
                    }
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        // save the shopping list to the database
        if (getDb().insertSL(shoppingList)) {
            sendResponse(exchange, 500, "Error loading shopping list on database!");
        }

        sendResponse(exchange, 200, "Shopping list created!");
    }
}
