package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import org.WishCloud.Database.SQl;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;

import java.io.IOException;
import java.util.Map;

public class DeleteHandler extends ServerHandler{

    public DeleteHandler(String serverName, Ring ring, SQl db) {
        super(serverName, ring, db);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get attributes from the exchange
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String,String> params = queryToMap(exchange.getRequestURI().getQuery());
        byte[] body = exchange.getRequestBody().readAllBytes();

        if (!method.equals("DELETE") || !path.equals("/delete")
                || !params.containsKey("uuid")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        // check if the shopping list is null or already exists in database
        ShoppingList shoppingList = getDb().read(params.get("uuid"));
        if (shoppingList == null) {
            sendResponse(exchange, 404, "Shopping list doesn't exist!");
            return;
        }

        if (getDb().write(shoppingList, "delete")) {
            sendResponse(exchange, 500, "Error deleting shopping list from database!");
            return;
        }

        sendResponse(exchange, 200, "Shopping list deleted!");
    }
}
