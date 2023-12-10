package org.WishCloud.Cloud.Handlers;

import org.WishCloud.Database.SQl;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.util.*;

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

        // check if the request is valid
        if (!method.equals("POST") || !path.equals("/create")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        // check if the shopping list is null or already exists in database
        ShoppingList shoppingList;
        if (getDb().read(params.get("uuid")) != null) {
            sendResponse(exchange, 409, "Shopping list already exists!");
            return;
        } else if ((shoppingList = Serializer.deserialize(body)) == null) {
            sendResponse(exchange, 400, "Shopping list corrupted!");
            return;
        } else if (getDb().write(shoppingList, "create")) {
            // when implementing hinted handoff, this will be changed to insertSL(shoppingList, params.get("hinted")))
            sendResponse(exchange, 500, "Error loading shopping list on database!");
            return;
        }

        if (!params.get("cord").equals("true")) { // if the request is not a cord request
            sendResponse(exchange, 200, "Shopping list created!");
            return;
        }

        List<String> servedNodes = put(body, "create", params.get("uuid"));

        if (servedNodes.size() != this.replicas) {
            servedNodes.remove(this.serverName);
            undoCreate(servedNodes);
            getDb().write(shoppingList, "delete");

            String response = "Error, not enough servers replicas! Served nodes: " + servedNodes.toString();
            sendResponse(exchange, 500, response);
            return;
        }

        sendResponse(exchange, 200, "Shopping list created!");
    }
}
