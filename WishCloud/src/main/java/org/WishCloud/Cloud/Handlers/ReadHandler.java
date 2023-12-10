package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import org.WishCloud.Database.SQl;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        // check if the shopping list is null or already exists in database
        ShoppingList shoppingList = getDb().read(params.get("uuid"));
        if (shoppingList == null) {
            sendResponse(exchange, 404, "Shopping list doesn't exist!");
            return;
        }

        if (!params.get("cord").equals("true")) {
            sendResponse(exchange, 200, Serializer.serialize(shoppingList));
            return;
        }

        AtomicReference<ShoppingList>  ref = new AtomicReference<>(shoppingList);
        int replicasRemaining = get(ref, params.get("uuid"));

        if (replicasRemaining != 0 || ref.get() == null) {
            sendResponse(exchange, 500, "Error retrieving shopping list!");
            return;
        }
        sendResponse(exchange, 200, Serializer.serialize(shoppingList));
    }
}