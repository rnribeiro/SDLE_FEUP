package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.WishCloud.Database.SQl;
import org.WishCloud.Utils.Ring;

import java.io.IOException;
import java.util.Map;

public class DeactivateHandler extends ServerHandler {
    private final HttpServer server;

    public DeactivateHandler(String serverName, Ring ring, SQl db, HttpServer server){
        super(serverName, ring, db);
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get attributes from the exchange
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String,String> params = queryToMap(exchange.getRequestURI().getQuery());
        byte[] body = exchange.getRequestBody().readAllBytes();

        if (!method.equals("GET") || !path.equals("/deactivate")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        // get server contexts
        this.server.removeContext("/read");
        this.server.removeContext("/update");
        this.server.removeContext("/create");
        this.server.removeContext("/refresh");

        System.out.println(serverName + " deactivated...");

        sendResponse(exchange, 200, serverName + " deactivated...");


    }
}