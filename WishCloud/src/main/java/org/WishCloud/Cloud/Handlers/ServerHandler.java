package org.WishCloud.Cloud.Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.WishCloud.Database.Backup;
import org.WishCloud.Database.Storage;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.net.http.HttpTimeoutException;


public abstract class ServerHandler implements HttpHandler {
    protected final int replicas = 3;
    protected final String serverName;
    protected final Ring ring;
    protected final Storage db;
    protected final Backup db_backup;

    public ServerHandler(String serverName, Ring ring, Storage db, Backup db_backup) {
        this.serverName = serverName;
        this.ring = ring;
        this.db = db;
        this.db_backup = db_backup;
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

    protected static void sendResponse(HttpExchange exchange, int statusCode, byte[] response) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(statusCode, 0);

            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected List<String> put(byte[] content, String uuid) {
        List<String> orderedNodes = getRing().getPreferenceList(uuid);
        List<String> preferenceList = getRing().getPreferenceList(uuid, this.replicas);
        Set<String> servedNodes = new HashSet<>();
        Set<String> hintedNodes = new HashSet<>();
        servedNodes.add(getServerName());
        for (String server : orderedNodes.subList(orderedNodes.indexOf(getServerName())+1, orderedNodes.size())) {
            // Check if the replica creation was successful
            if (servedNodes.size() == this.replicas) { break; }

            String hintedNode = null;
            StringBuilder url = new StringBuilder("http://" + server + "/upload" + "?uuid=" + uuid + "&cord=false");
            if (!preferenceList.contains(server)) {
                // find server in the preference list that was not served yet
                hintedNode = preferenceList.stream()
                        .filter(node -> !servedNodes.contains(node) && !hintedNodes.contains(node))
                        .findFirst().orElse(null);
                url.append("&hinted=").append(hintedNode);
            }

            // send the shopping list to the next server in the ring
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(java.time.Duration.ofMillis(400))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    System.out.println("\nReplica in " + server + " upload! Server Response: " + response.body());
                    servedNodes.add(server);
                    if (hintedNode != null) { hintedNodes.add(hintedNode); }
                }
            }

            catch (InterruptedException | IOException e) {
                System.out.println(e.getMessage());
            }
        }

        return servedNodes.stream().toList();
    }

    protected int get(AtomicReference<ShoppingList> ref, String uuid) {
        ShoppingList shoppingList = ref.get();
        int replicasRemaining = this.replicas - 1;
        List<String> orderedList = getRing().getPreferenceList(uuid);
        for (String server : orderedList.subList(orderedList.indexOf(getServerName()) + 1, orderedList.size())) {
            if (replicasRemaining == 0) { break; }

            // send the shopping list to the next server in the ring
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + server + "/read?uuid=" + uuid + "&cord=false"))
                    .timeout(java.time.Duration.ofMillis(400))
                    .GET()
                    .build();

            try {
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    System.out.println("\nReplica in " + server + " read! Server Response: " + Arrays.toString(response.body()));
                    ShoppingList newSL = Serializer.deserialize(response.body());
                    shoppingList = shoppingList.merge(newSL.getListItems());
                    replicasRemaining--;
                }
            } catch (InterruptedException | IOException e) {
                System.out.println(e.getMessage());
            }
        }

        ref.set(shoppingList);
        return replicasRemaining;
    }

    public Storage getDb() {
        return db;
    }

    public Backup getDb_backup() {
        return db_backup;
    }

    public Ring getRing() { return ring; }

    public String getServerName() { return serverName; }
}
