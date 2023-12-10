package org.WishCloud.Cloud.Handlers;

import org.WishCloud.Database.SQl;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.util.*;

import com.sun.net.httpserver.HttpExchange;

public class UploadHandler extends ServerHandler {
    private final SQl db_hinted;

    public UploadHandler(String serverName, Ring ring, SQl db, SQl db_hinted) {
        super(serverName, ring, db);
        this.db_hinted = db_hinted;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get attributes from the exchange
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String,String> params = queryToMap(exchange.getRequestURI().getQuery());
        byte[] body = exchange.getRequestBody().readAllBytes();

        // check if the request is valid
        if (!method.equals("POST") || !path.equals("/upload")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        // check if the remote shopping list is null or already exists in database
        ShoppingList remoteSL;
        boolean cord = params.get("cord").equals("true");
        boolean prefList = getRing().getPreferenceList(params.get("uuid"), this.replicas).contains(this.serverName);
        String hinted = params.getOrDefault("hinted", null);

        if (!prefList && cord) {
            sendResponse(exchange, 404, "Does not belong to this server!");
            return;
        } else if ((remoteSL = Serializer.deserialize(body)) == null) {
            sendResponse(exchange, 400, "Shopping list corrupted!");
            return;
        }

        if (hinted == null) {
            ShoppingList localSL = getDb().read(params.get("uuid"));
            if (localSL == null) {
                if (getDb().write(remoteSL, "insert")) {
                    sendResponse(exchange, 500, "Error creating shopping list on database!");
                    return;
                }
            } else {
                localSL = localSL.merge(remoteSL.getListItems());
                if (getDb().write(localSL, "update")) {
                    sendResponse(exchange, 500, "Error updating shopping list on database!");
                    return;
                }
            }
        } else {
            // check if hinted address is in preference list
            if (!getRing().getPreferenceList(params.get("uuid"), this.replicas).contains(hinted)) {
                sendResponse(exchange, 404, "Hinted address is not in preference list!");
                return;
            }

            // find pair hint uuid in database
            // if list exists check if hint is the same for hinted
            // merge and update
            // else insert in case no list of uuid exists
        }

        if (!cord) { // if the request is not a cord request
            sendResponse(exchange, 200, "Shopping list uploaded!");
            return;
        }

        List<String> servedNodes = put(body, params.get("uuid"));

        if (servedNodes.size() != this.replicas) {
            String response = "Error, not enough replicas were uploaded! Served nodes: " + servedNodes.toString();
            sendResponse(exchange, 500, response);
            return;
        }

        sendResponse(exchange, 200, "Shopping list created!");
    }
}
