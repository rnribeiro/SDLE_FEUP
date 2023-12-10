package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.WishCloud.Database.SQl;
import org.WishCloud.Utils.Ring;

import java.io.IOException;
import java.util.Map;

public class ActivateHandler extends ServerHandler {
    private final HttpServer server;

    public ActivateHandler(String serverName, Ring ring, SQl db, HttpServer server){
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

        if (!method.equals("GET") || !path.equals("/activate")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }

        server.createContext("/create", new CreateHandler(this.serverName, ring, db));
        server.createContext("/read", new ReadHandler(this.serverName, ring, db));
        server.createContext("/update", new UpdateHandler(this.serverName, ring, db));
        server.createContext("/refresh", new RefreshHandler());

        System.out.println(serverName + " activated...");

        sendResponse(exchange, 200, serverName + " activated...");


    }
}