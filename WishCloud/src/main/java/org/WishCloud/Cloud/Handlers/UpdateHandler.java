package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import org.WishCloud.Database.SQl;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
        ShoppingList localSL = getDb().read(params.get("uuid"));
        ShoppingList clientSl = Serializer.deserialize(body);
        if (localSL == null) {
            sendResponse(exchange, 404, "Shopping list doesn't exist!");
            return;
        } else if (clientSl == null) {
            sendResponse(exchange, 400, "Shopping list corrupted!");
            return;
        }

        ShoppingList mergedSL = localSL.merge(clientSl.getListItems());
        if (getDb().write(mergedSL, "update")) {
            sendResponse(exchange, 500, "Error updating shopping list on database!");
            return;
        }

        if (!params.get("cord").equals("true")) {
            sendResponse(exchange, 200, "Shopping list updated!");
            return;
        }

        List<String> servedNodes = put(Serializer.serialize(mergedSL), "update", params.get("uuid"));
        if (servedNodes.size() != this.replicas) {
            sendResponse(exchange, 500, "Error updating replicas!");
            return;
        }

        sendResponse(exchange, 200, "Shopping list updated!");
    }
}
