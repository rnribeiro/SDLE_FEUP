package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReadHandler extends ServerHandler {
    public ReadHandler(String serverName, Ring ring, SQl db) {
        super(serverName, ring, db);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get attributes from the exchange
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String,String> params = queryToMap(exchange.getRequestURI().getQuery());
        byte[] body = exchange.getRequestBody().readAllBytes();

        if (!method.equals("GET") || !path.equals("/read")
                || !params.containsKey("uuid")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }


        // check if the shopping list is null or already exists in database
        ShoppingList shoppingList = getDb().getShoppingList(params.get("uuid"));
        if (shoppingList == null) {
            sendResponse(exchange, 400, "Shopping doesn't exist!");
            return;
        }

        int replicas = 2;
        List<String> preferenceList = getRing().getPreferenceList(params.get("uuid"), replicas);
        if (preferenceList.get(0).equals(getServerName())) {
            for (String server : preferenceList.subList(1, preferenceList.size())) {
                // send the shopping list to the next server in the ring
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/read?uuid=" + params.get("uuid")))
                        .GET()
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        System.out.println("\nReplica in " + server + " created! Server Response: " + response.body());
                        ShoppingList newSL = Serializer.deserialize(response.body().getBytes());
                        shoppingList.merge(newSL.getListItems());
                        replicas--;
                    }
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        sendResponse(exchange, 200, Arrays.toString(Serializer.serialize(shoppingList)));
    }
}