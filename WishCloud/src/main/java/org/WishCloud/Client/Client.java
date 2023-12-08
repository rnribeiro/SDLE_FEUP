package org.WishCloud.Client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.WishCloud.CRDT.CRDT;
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
//        clientUUID = "64c4090b-5ccf-47a2-a492-9c03794a5b35";
        System.out.println("Client UUID: " + clientUUID);

        db = new SQl(clientUUID); // Create a database instance specific to this client
        db.createDB(); // Create the database
        db.connect(); // Connect to the database
        Scanner scanner = new Scanner(System.in);

        mainMenu(scanner);

    }

    public static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private static void mainMenu(Scanner scanner) {
        while (true) {
            ShoppingInterface.printMainMenu();

            // try to read user input as an integer
            int choice;
            try {
                choice = scanner.nextInt();
            } catch (Exception e) {
                ShoppingInterface.displayInvalidChoice();
                scanner.next();
                continue;
            }

            switch (choice) {
                case 1:
                    handleCreateList(scanner);
                    break;
                case 2:
                    handleAccessList(scanner, null);
                    break;
                default:
                    ShoppingInterface.displayInvalidChoice();
            }
        }
    }

    private static void handleCreateList(Scanner scanner) {
        String listName = ShoppingInterface.promptForListName(scanner);
        String listUUID = UUID.randomUUID().toString();

        ShoppingList shoppingList = new ShoppingList(listName, listUUID, new HashMap<>());
        db.connect();
        db.insertSL(shoppingList);
//        db.close();
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
                        .uri(URI.create("http://" + server + "/create?uuid=" + listUUID + "&cord=true"))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(serializedList))
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        System.out.println("\nReplica in " + server + " created! Server Response: " + response.body());
                        return true;
                    } else {
                        System.out.println("\nReplica in " + server + " failed! Server Response: " + response.body());
                    }

                } catch (ConnectException | InterruptedException e) {
                    System.out.println(e.getMessage());
                }

            }
            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static ShoppingList getListFromServerOrLocal(String listUUID) {
        ShoppingList shoppingList = getListFromServer(listUUID);
        ShoppingList list = null;
        if (shoppingList == null) {
            db.connect();
            list = db.getShoppingList(listUUID);
//            db.close();
        } else {
            // merge the local list with the server list
            db.connect();
            ShoppingList localList = db.getShoppingList(listUUID);
//            db.close();
            if (localList != null) {
                list = shoppingList.merge(localList.getListItems());
            }
        }

        return list;
    }

    public static void handleAccessList(Scanner scanner, String listUUID) {
        if (listUUID == null) {
            listUUID = ShoppingInterface.promptForListUUID(scanner);
        }
        ShoppingList shoppingList = getListFromServerOrLocal(listUUID);

        if (shoppingList != null) {
            ShoppingInterface.displayShoppingList(shoppingList);
            // Prompt user for list actions
            while (true) {
                System.out.println("List Actions:");
                System.out.println("1- Add Item");
                System.out.println("2- Remove Item");
                System.out.println("3- Update Item");
                System.out.println("4- Exit");
                System.out.print("Enter your choice: ");
                int choice;
                try {
                    choice = scanner.nextInt();
                } catch (Exception e) {
                    ShoppingInterface.displayInvalidChoice();
                    scanner.next();
                    continue;
                }

                switch (choice) {
                    case 1:
                        handleAddItem(scanner, shoppingList);
                        break;
                    case 2:
                        handleRemoveItem(scanner, shoppingList);
                        break;
                    case 3:
                        handleUpdateItem(scanner, shoppingList);
                        break;
                    case 4:
                        mainMenu(scanner);
                    default:
                        ShoppingInterface.displayInvalidChoice();
                }
            }
        } else {
            ShoppingInterface.displayListNotFound();
        }
    }

    private static void handleUpdateItem(Scanner scanner, ShoppingList shoppingList) {
        System.out.print("Enter Item Name: ");
        String itemName = scanner.next();
        System.out.print("Enter New Item Value: ");
        int itemValue = scanner.nextInt();

        // create CRDT object
        CRDT<String> crdtItem = new CRDT<>(Integer.toString(itemValue), System.currentTimeMillis(), clientUUID);
        shoppingList.updateItem(itemName, crdtItem);

        // try updating the list in the database
        db.connect();
        if (!db.updateShoppingList(shoppingList)) {
            System.out.println("Item locally updated successfully.");
        } else {
            System.out.println("Failed to update item.");
        }
//        db.close();
        byte[] serializedList = Serializer.serialize(shoppingList);
        boolean syncSuccess = updateListInCloud(shoppingList.getListID(), serializedList);
        ShoppingInterface.displayItemUpdateSuccess(syncSuccess);
        handleAccessList(scanner, shoppingList.getListID());
    }

    private static void handleRemoveItem(Scanner scanner, ShoppingList shoppingList) {
        System.out.print("Enter Item Name: ");
        String itemName = scanner.next();
        shoppingList.removeItem(itemName);

        // try updating the list in the database
        db.connect();
        if (!db.updateShoppingList(shoppingList)) {
            System.out.println("Item locally removed successfully.");
        } else {
            System.out.println("Failed to remove item.");
        }
//        db.close();

        byte[] serializedList = Serializer.serialize(shoppingList);
        boolean syncSuccess = updateListInCloud(shoppingList.getListID(), serializedList);
        ShoppingInterface.displayItemRemovalSuccess(syncSuccess);
        handleAccessList(scanner, shoppingList.getListID());
    }

    private static boolean updateListInCloud(String listID, byte[] serializedList) {
        try {
            List<String> preferenceList = ring.getPreferenceList(listID, 3);
            for (String server : preferenceList) {

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/update?uuid=" + listID + "&cord=true"))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(serializedList))
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        System.out.println("\nReplica in " + server + " updated! Server Response: " + response.body());
                        return true;
                    } else {
                        System.out.println("\nReplica in " + server + " failed! Server Response: " + response.body());
                    }
                } catch (ConnectException | InterruptedException e) {
                    System.out.println("\nReplica in " + server + " failed!" + e.getMessage());
                }

            }
            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void handleAddItem(Scanner scanner, ShoppingList shoppingList) {
        System.out.print("Enter Item Name: ");
        String itemName = scanner.next();
        System.out.print("Enter Item Value: ");
        int itemValue = scanner.nextInt();

        // create CRDT object
        CRDT<String> crdtItem = new CRDT<>(Integer.toString(itemValue), System.currentTimeMillis(), clientUUID);
        shoppingList.addItem(itemName, crdtItem);

        // try updating the list in the database
        db.connect();
        if (!db.updateShoppingList(shoppingList)) {
            System.out.println("Item locally added successfully.");
        } else {
            System.out.println("Failed to add item.");
        }
//        db.close();


        byte[] serializedList = Serializer.serialize(shoppingList);
        boolean syncSuccess = updateListInCloud(shoppingList.getListID(), serializedList);
        ShoppingInterface.displaySynchronizationStatus(syncSuccess);
        handleAccessList(scanner, shoppingList.getListID());
    }

    private static ShoppingList getListFromServer(String listUUID) {
        try {
            List<String> preferenceList = ring.getPreferenceList(listUUID, 3);
            int serversDown = 0;
            for (String server : preferenceList) {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/read?uuid=" + listUUID))
                        .GET()
                        .build();

                try {
                    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() == 200) {
                        // Read the InputStream into a byte array
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[1024];
                        while ((nRead = response.body().read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }

                        // Deserialize the byte array into a ShoppingList
                        byte[] serializedList = buffer.toByteArray();
                        return Serializer.deserialize(serializedList);
                    }
                } catch (InterruptedException | ConnectException e) {
                    serversDown++;
                }
            }
            if (serversDown == 3) {
                System.out.println("All servers are down. Please try again later.");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
