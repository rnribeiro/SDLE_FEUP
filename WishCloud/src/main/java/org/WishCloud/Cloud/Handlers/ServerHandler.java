package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.WishCloud.Database.SQl;
import org.WishCloud.Utils.Ring;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class ServerHandler implements HttpHandler {
    protected final int replicas = 3;
    protected final String serverName;
    protected final Ring ring;
    protected final SQl db;

    public ServerHandler(String serverName, Ring ring, SQl db) {
        this.serverName = serverName;
        this.ring = ring;
        this.db = db;
    }

    protected static Map<String, String> queryToMap(String query){

        Map<String, String> result = new HashMap<String, String>();

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length>1) { result.put(pair[0], pair[1]); }
            else{ result.put(pair[0], ""); }
        }

        return result;

    }

    protected static void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(statusCode, 0);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public SQl getDb() {
        return db;
    }

    public Ring getRing() { return ring; }

    public String getServerName() { return serverName; }
}
