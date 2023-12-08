package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpdateHandler extends ServerHandler {

    public UpdateHandler(String serverName, Ring ring, SQl db) {
        super(serverName, ring, db);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get attributes from the exchange
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String,String> params = queryToMap(exchange.getRequestURI().getQuery());
        byte[] body = exchange.getRequestBody().readAllBytes();

        if (!method.equals("POST") || !path.equals("/update")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        // check if the shopping list is null or already exists in database
        ShoppingList localSL = getDb().getShoppingList(params.get("uuid"));
        ShoppingList clientSl = Serializer.deserialize(body);
        if (localSL == null) {
            sendResponse(exchange, 400, "Shopping list doesn't exist!");
//            // build create request
//            HttpClient client = HttpClient.newHttpClient();
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("http://" + getServerName() + "/create?uuid=" + params.get("uuid") + "&cord=false"))
//                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
//                    .build();
//
//            try {
//                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//                if (response.statusCode() == 200) {
//                    System.out.println("\nReplica in " + getServerName() + " created! Server Response: " + response.body());
//                }
//            } catch (InterruptedException e) {
//                System.out.println(e.getMessage());
//            } finally {
//                sendResponse(exchange, 200, "Shopping list created!");
//            }
            return;
        } else if (clientSl == null) {
            sendResponse(exchange, 400, "Shopping list corrupted!");
            return;
        }

        ShoppingList mergedSL = localSL.merge(clientSl.getListItems());
        if (getDb().updateShoppingList(mergedSL)) {
            sendResponse(exchange, 500, "Error updating shopping list on database!");
            return;
        }

        if (!params.get("cord").equals("true")) { return; }

        List<String> orderedNodes = getRing().getPreferenceList(params.get("uuid"));
        List<String> preferenceList = getRing().getPreferenceList(params.get("uuid"), this.replicas);
        Set<String> servedNodes = new HashSet<>();
        Set<String> hintedNodes = new HashSet<>();
        servedNodes.add(getServerName());
        for (String server : orderedNodes.subList(orderedNodes.indexOf(getServerName())+1, orderedNodes.size())) {
            // Check if the replica creation was successful
            if (servedNodes.size() == this.replicas) { break; }

            String hintedNode = null;
            StringBuilder url = new StringBuilder("http://" + server + "/update?uuid=" + params.get("uuid") + "&cord=false");
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
                    System.out.println("\nReplica in " + server + " updated! Server Response: " + response.body());
                    servedNodes.add(server);
                    if (hintedNode != null) { hintedNodes.add(hintedNode); }
                }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        if (servedNodes.size() != this.replicas) {
            sendResponse(exchange, 500, "Error updating replicas!");
            return;
        }

        sendResponse(exchange, 200, "Shopping list updated!");
    }
}
