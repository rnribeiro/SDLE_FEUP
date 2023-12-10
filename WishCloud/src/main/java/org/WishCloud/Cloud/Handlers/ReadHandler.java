package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import org.WishCloud.Database.Backup;
import org.WishCloud.Database.Storage;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ReadHandler extends ServerHandler {
    public ReadHandler(String serverName, Ring ring, Storage db, Backup db_hinted) {
        super(serverName, ring, db, db_hinted);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get attributes from the exchange
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String,String> params = queryToMap(exchange.getRequestURI().getQuery());
        byte[] body = exchange.getRequestBody().readAllBytes();

        // check if the request is valid
        if (!method.equals("GET") || !path.equals("/read")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        // stores role flags
        boolean cord = params.get("cord").equals("true");
        boolean prefList = getRing().getPreferenceList(params.get("uuid"), this.replicas).contains(this.serverName);

        // check if the server is the coordinator and belongs to item preference list
        if (!prefList && cord) {
            sendResponse(exchange, 404, "Does not belong to this server!");
            return;
        }

        // retrieve replica from database
        ShoppingList localSL;
        if (!prefList) {
            AtomicReference<String> ref = new AtomicReference<>(null);
            // read from hinted database
            localSL = getDb_backup().read(params.get("uuid"), ref);
        } else {
            localSL = getDb().read(params.get("uuid"));
        }

        // check if shopping list exists
        if (localSL == null) {
            sendResponse(exchange, 404, "Shopping list doesn't exist!");
            return;
        }

        // check if server is coordinator
        if (!cord) {
            sendResponse(exchange, 200, Serializer.serialize(localSL));
            return;
        }

        // do coordinator job
        AtomicReference<ShoppingList>  ref = new AtomicReference<>(localSL);
        int replicasRemaining = get(ref, params.get("uuid"));

        // check if enough replicas were retrieved
        if (replicasRemaining != 0 || ref.get() == null) {
            sendResponse(exchange, 500, "Error retrieving shopping list!");
            return;
        }
        sendResponse(exchange, 200, Serializer.serialize(localSL));
    }
}