package org.WishCloud.Client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import org.WishCloud.Client.UI.ShoppingInterface;
import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;


public class Client {

    private static String clientUUID;
    private static SQl db;
    private static Ring ring;
    private static List<String> seeds;
    private static final int HashSpace = 1 << 31;

    public static void main(String[] args) {

        seeds = List.of("localhost:8000", "localhost:8001", "localhost:8002");
        ring = new Ring(HashSpace);

        for (String seed : seeds) { ring.addNode(seed, 10); }

        clientUUID = generateUUID();
        System.out.println("Client UUID: " + clientUUID);

        db = new SQl(clientUUID); // Create a database instance specific to this client
        db.createDB(); // Create the database
        db.connect();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            ShoppingInterface.printMainMenu();
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    handleCreateList(scanner);
                    break;
                case 2:
                    handleAccessList(scanner);
                    break;
                default:
                    ShoppingInterface.displayInvalidChoice();
            }
        }
    }

    public static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private static void handleCreateList(Scanner scanner) {
        String listName = ShoppingInterface.promptForListName(scanner);
        String listUUID = UUID.randomUUID().toString();

        ShoppingList shoppingList = new ShoppingList(listName, listUUID, new HashMap<>());
        db.insertSL(shoppingList);

        byte[] serializedList = Serializer.serialize(shoppingList);
        boolean syncSuccess = synchronizeListWithServer(listUUID, serializedList);

        ShoppingInterface.displayCreationSuccess(listUUID);
        ShoppingInterface.displaySynchronizationStatus(syncSuccess);
    }

    private static boolean synchronizeListWithServer(String listUUID, byte[] serializedList) {
        try {
            List<String> preferenceList = ring.getPreferenceList(listUUID, 3);
            for (String server : preferenceList) {

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/create?uuid=" + listUUID))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(serializedList))
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        System.out.println("\nReplica in " + server + " created! Server Response: " + response.body());
                        return true;
                    }
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private static ShoppingList getListFromServerOrLocal(String listUUID) {
        ShoppingList shoppingList = getListFromServer(listUUID);
        if (shoppingList == null) {
            db.connect();
            shoppingList = db.getShoppingList(listUUID);
            db.close();
        }
        return shoppingList;
    }

    public static void handleAccessList(Scanner scanner) {
        String listUUID = ShoppingInterface.promptForListUUID(scanner);
        ShoppingList shoppingList = getListFromServerOrLocal(listUUID);

        if (shoppingList != null && !shoppingList.getListItems().isEmpty()) {
            ShoppingInterface.displayShoppingList(shoppingList);
        } else {
            ShoppingInterface.displayListNotFound();
        }
    }

    private static ShoppingList getListFromServer(String listUUID) {
        try {
            URL url = new URL("http://" + SERVER_IP + ":" + SERVER_PORT + "/read?uuid=" + URLEncoder.encode(listUUID, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream inputStream = conn.getInputStream()) {
                    int nRead;
                    byte[] data = new byte[1024];
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                }
                byte[] serializedList = buffer.toByteArray();
                return Serializer.deserialize(serializedList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



}
