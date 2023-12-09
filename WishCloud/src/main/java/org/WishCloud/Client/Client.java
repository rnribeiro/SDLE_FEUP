package org.WishCloud.Client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

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

        ring = new Ring(HashSpace);

        int numberOfSeeds; // Change this to the desired number of seeds
        if (args.length > 0) {
            numberOfSeeds = Integer.parseInt(args[0]);
        } else {
            numberOfSeeds = 3;
        }
        seeds = new ArrayList<>();

        for (int i = 0; i < numberOfSeeds; i++) {
            seeds.add("localhost:80" + (i < 10 ? "0" + i : i));
        }

        for (String seed : seeds) {
            ring.addNode(seed, 10);
        }

        // get client UUID from args or generate a new one
        if (args.length > 1) {
            clientUUID = args[1];
        } else {
            clientUUID = generateUUID();
        }
        ShoppingInterface.clearConsole();
        System.out.println("Client UUID: " + clientUUID);

        db = new SQl(clientUUID); // Create a database instance specific to this client
        db.createDB();
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
        ShoppingInterface.displayCreationSuccess(listUUID, db.insertSL(shoppingList));
        byte[] serializedList = Serializer.serialize(shoppingList);
        boolean syncSuccess = synchronizeListWithServer(listUUID, serializedList);

        ShoppingInterface.displaySynchronizationStatus(syncSuccess);
    }

    private static boolean synchronizeListWithServer(String listUUID, byte[] serializedList) {
        try {

            System.out.println("\nAttempting to synchronize list with cloud...");
            List<String> preferenceList = ring.getPreferenceList(listUUID, 3);
            // print the preference list
            System.out.println("\nPreference List:");
            for (String server : preferenceList) {
                System.out.println(server);
            }
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
        ShoppingList list;
        if (shoppingList == null) {
            // print getting list from local
            System.out.println("\nAttempting to get list from local database...");
            list = db.getShoppingList(listUUID);
        } else {
            // merge the local list with the server list
            System.out.println("\nAttempting to get list from local database...");
            ShoppingList localList = db.getShoppingList(listUUID);
            if (localList != null) {
                // print merging lists
                System.out.println("\nMerging lists...");
                list = shoppingList.merge(localList.getListItems());
                db.updateShoppingList(list);
            } else {
                db.insertSL(shoppingList);
                list = shoppingList;
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
                System.out.println("2- Update Item");
                System.out.println("3- Exit");
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
                        handleUpdateItem(scanner, shoppingList);
                        break;
                    case 3:
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

        if (!db.updateShoppingList(shoppingList)) {
            System.out.println("Item locally updated successfully.");
        } else {
            System.out.println("Failed to update item.");
        }
        byte[] serializedList = Serializer.serialize(shoppingList);
        boolean syncSuccess = updateListInCloud(shoppingList.getListID(), serializedList);
        ShoppingInterface.displayItemUpdateSuccess(syncSuccess);
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
                        System.out.println("\nFailed to update replica in " + server + "! Server Response: " + response.body());
                    }
                } catch (ConnectException | InterruptedException e) {
                    System.out.println("\nFailed to update replica in " + server + "!" + e.getMessage());
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
        if (!db.updateShoppingList(shoppingList)) {
            System.out.println("Item locally added successfully.");
        } else {
            System.out.println("Failed to add item.");
        }

        byte[] serializedList = Serializer.serialize(shoppingList);
        boolean syncSuccess = updateListInCloud(shoppingList.getListID(), serializedList);
        ShoppingInterface.displaySynchronizationStatus(syncSuccess);
        handleAccessList(scanner, shoppingList.getListID());
    }

    private static ShoppingList getListFromServer(String listUUID) {
        try {
            // getting list from server
            System.out.println("\nAttempting to get list from cloud...");
            List<String> preferenceList = ring.getPreferenceList(listUUID, 3);
            // print the preference list
            System.out.println("\nPreference List:");
            for (String server : preferenceList) {
                System.out.println(server);
            }
            int serversDown = 0;
            for (String server : preferenceList) {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/read?uuid=" + listUUID + "&cord=true"))
                        .GET()
                        .build();

                try {
                    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() == 200) {
                        return Serializer.deserialize(response.body());
                    } else if (response.statusCode() == 404) {
                        System.out.println("\nList not found in " + server + "!");
                        ShoppingList localList = db.getShoppingList(listUUID);
                        if (localList != null) {
                            synchronizeListWithServer(listUUID, Serializer.serialize(localList));
                        }
                    } else {
                        System.out.println(response.statusCode());
                        System.out.println("\nFailed to read from server " + server + "! Server Response: " + Arrays.toString(response.body()));

                    }
                } catch (InterruptedException | ConnectException e) {
                    serversDown++;
                }
            }
            if (serversDown == 3) {
                System.out.println("\nAll servers are down. Please try again later.");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
