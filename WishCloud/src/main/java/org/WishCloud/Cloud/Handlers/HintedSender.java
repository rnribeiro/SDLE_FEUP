package org.WishCloud.Cloud.Handlers;

import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Database.Backup;
import org.WishCloud.Utils.Pair;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class HintedSender implements Runnable {
    private boolean exit;
    private final String serverName;
    private final Backup db_backup;

    private final Thread t;

    public HintedSender(String serverName, Backup db_backup) {
        exit = false;
        this.serverName = serverName;
        this.db_backup = db_backup;
        this.t = new Thread(this, "HintedSender");
        t.start();
    }

    @Override
    public void run() {
        while (!exit) {
            // retrieve hinted data
            List<Pair<ShoppingList,String>> hintedData = db_backup.readAll();
            for (Pair<ShoppingList,String> hint : hintedData) {
                ShoppingList shoppingList = hint.getLeft();
                String server = hint.getRight();
                String url = "http://" + server + "/upload" + "?uuid=" + shoppingList.getListID() + "&cord=false";
                // send the shopping list to the next server in the ring
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(shoppingList)))
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        System.out.println("\nReplica in " + server + " upload! Server Response: " + response.body());
                        if (db_backup.write(shoppingList, "delete", server)) {
                            System.out.println("\nDelete operation in server " + this.serverName + " failed!");
                        }
                    } else {
                        System.out.println("\nReplica in " + server + " upload failed! Server Response: " + response.body());
                    }
                } catch (InterruptedException | IOException e) {
                    System.out.println(e.getMessage());
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopThread()
    {
        exit = true;
    }

}
