package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class ReadHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Get the output stream to write the response
        OutputStream os = exchange.getResponseBody();

        // Write the response
        String response = "Hello, this is a simple HTTP server!";
        os.write(response.getBytes());

        // Close the output stream
        os.close();
    }
}