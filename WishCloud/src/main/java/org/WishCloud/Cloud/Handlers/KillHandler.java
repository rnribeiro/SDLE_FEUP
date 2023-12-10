package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KillHandler extends ServerHandler {
    private final HttpServer server;

    public KillHandler(String serverName, Ring ring, SQl db, HttpServer server){
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

        if (!method.equals("GET") || !path.equals("/kill")
                || !params.containsKey("uuid") || !params.containsKey("cord")) {
            sendResponse(exchange, 400, "Invalid request!");
            return;
        }
        sendResponse(exchange, 200, "Killed...");

        System.out.println(serverName + " shutting down");
        this.server.stop(2);
        System.out.println(serverName + " stopped");


    }
}