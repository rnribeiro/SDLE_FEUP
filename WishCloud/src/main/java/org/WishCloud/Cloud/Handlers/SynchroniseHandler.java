package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.WishCloud.Database.SQl;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;
import org.WishCloud.ShoppingList.ShoppingList;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SynchroniseHandler implements HttpHandler {

    private final Ring ring;
    private final HttpClient httpClient;

    public SynchroniseHandler(Ring ring) {
        this.ring = ring;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Get the output stream to write the response
        OutputStream os = exchange.getResponseBody();

        // Read the serialized shopping list from the request body
        byte[] requestBodyBytes = exchange.getRequestBody().readAllBytes();
        ShoppingList clientShoppingList = Serializer.deserialize(requestBodyBytes);

        // Check if the shopping list exists in the server's database
        SQl db = new SQl("your_database_name.db");
        ShoppingList serverShoppingList = db.getShoppingList(clientShoppingList.getListID());

        if (serverShoppingList != null) {
            // Merge the CRDT maps of client and server shopping lists
            ShoppingList mergedShoppingList = clientShoppingList.merge(serverShoppingList.getListItems());

            // Update the server's database with the merged shopping list
            db.updateShoppingList(mergedShoppingList);

            // Serialize the merged shopping list to send back to the client
            byte[] responseBytes = Serializer.serialize(mergedShoppingList);

            // Set response headers
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            // Send response
            exchange.sendResponseHeaders(200, responseBytes.length);
            os.write(responseBytes);

            // Propagate the updated shopping list to the next nodes in the ring using HttpClient
            propagateUpdate(mergedShoppingList);
        } else {
            // Handle the case where the server shopping list does not exist
            String response = "Server shopping list not found.";
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }

        // Close the output stream
        os.close();
    }

    private void propagateUpdate(ShoppingList updatedShoppingList) {
        List<String> nextNodes = ring.getNextNodes(ring.getNode(updatedShoppingList.getListID()), 3);
        for (String nextNode : nextNodes) {
            try {
                // Create an HTTP request to the next node in the ring
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + nextNode + "/update"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(updatedShoppingList)))
                        .build();

                // Send the request using HttpClient
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // Get the response from the next node (optional)
                int responseCode = response.statusCode();
                System.out.println("Update propagated to " + nextNode + ". Response code: " + responseCode);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                // Handle connection issues or errors during propagation
            }
        }
    }

}
