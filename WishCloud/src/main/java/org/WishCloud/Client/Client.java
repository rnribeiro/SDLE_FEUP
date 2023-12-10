package org.WishCloud.Client;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.Client.UI.ShoppingInterface;
import org.WishCloud.Database.SQl;
import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.Utils.Ring;
import org.WishCloud.Utils.Serializer;


public class Client {

    private static String clientUUID;
    private static SQl db;
    private static Ring ring;
    private static List<String> seeds;
    private static final int HashSpace = 1 << 31;
    private static Timer timer;

    public static void main(String[] args) {

        ring = new Ring(HashSpace);

        int numberOfSeeds;
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

        db = new SQl(clientUUID);
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
            // print main menu
            ShoppingInterface.printMainMenu();

            int choice;
            try {
                choice = scanner.nextInt();
            } catch (Exception e) {
                ShoppingInterface.displayInvalidChoice();
                scanner.next();
                continue;
            }

            // handle main menu choice
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

        // create the shopping list
        ShoppingList shoppingList = new ShoppingList(listName, listUUID, new HashMap<>());
        ShoppingInterface.displayCreationSuccess(listUUID, db.write(shoppingList, "insert"));

        // serialize the shopping list
        byte[] serializedList = Serializer.serialize(shoppingList);

        // synchronize the list with the cloud
        boolean syncSuccess = synchronizeListWithServer(listUUID, serializedList);

        // display synchronization status
        ShoppingInterface.displaySynchronizationStatus(syncSuccess);
    }

    private static boolean synchronizeListWithServer(String listUUID, byte[] serializedList) {
        try {
            ShoppingInterface.printSyncAttempt();

            // get the preference list
            List<String> preferenceList = ring.getPreferenceList(listUUID, 3);

            ShoppingInterface.printShoppingList(preferenceList);

            // loop through the preference list and try to create the list in the cloud
            for (String server : preferenceList) {

                // create http client and request
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/create?uuid=" + listUUID + "&cord=true"))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(serializedList))
                        .build();

                // send the request
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // check the response code
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
            list = db.read(listUUID);
            synchronizeListWithServer(listUUID, Serializer.serialize(list));
        } else {
            // merge the local list with the server list
            System.out.println("\nAttempting to get list from local database...");
            ShoppingList localList = db.read(listUUID);
            if (localList != null) {
                // print merging lists
                System.out.println("\nMerging lists...");
                list = shoppingList.merge(localList.getListItems());
                db.write(list, "update");
                updateListInCloud(listUUID, Serializer.serialize(list));

            } else {
                db.write(shoppingList, "insert");
                list = shoppingList;
            }
        }

        return list;
    }

    private static class ScannerThread extends Thread {
        private final Scanner scanner;
        private int choice;

        public ScannerThread(Scanner scanner) {
            this.scanner = scanner;
        }

        public int getChoice() {
            return choice;
        }

        @Override
        public void run() {
            try {
                choice = scanner.nextInt();
            } catch (Exception e) {
                choice = -1; // Set a special value to indicate an error
                scanner.next(); // Consume invalid input
            }
        }
    }


    public static void handleAccessList(Scanner scanner, String listUUID) {

        // prompt user for list UUID if not coming from list menu
        if (listUUID == null) {
            listUUID = ShoppingInterface.promptForListUUID(scanner);
        }

        // get the list from the server or local database
        ShoppingList shoppingList = getListFromServerOrLocal(listUUID);

        if (shoppingList != null) {
            ShoppingInterface.clearConsole();
            timer = new Timer(true);

            displayAndRefreshListPeriodically(scanner, shoppingList);

            // Prompt user for list actions
            while (true) {
                ScannerThread scannerThread = new ScannerThread(scanner);
                scannerThread.start(); // Start a new thread for the next input
                // Wait for the user input thread to finish
                try {
                    scannerThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int choice = scannerThread.getChoice();

                switch (choice) {
                    case 1:
                        timer.cancel();
                        scannerThread.interrupt();
                        handleAddItem(new Scanner(System.in), shoppingList);
                        break;
                    case 2:
                        timer.cancel();
                        scannerThread.interrupt();
                        handleUpdateItem(new Scanner(System.in), shoppingList);
                        break;
                    case 3:
                        timer.cancel();
                        scannerThread.interrupt();
                        ShoppingInterface.clearConsole();
                        mainMenu(scanner);
                        break;
                    default:
                        ShoppingInterface.displayInvalidChoice();
                        break;
                }
            }
        } else {
            ShoppingInterface.displayListNotFound();
        }
    }


    private static void displayAndRefreshListPeriodically(Scanner scanner, ShoppingList shoppingList) {
        // Schedule a task to refresh and display the list every 5 seconds
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // refresh the list
                ShoppingInterface.clearConsole();
                ShoppingInterface.displayShoppingList(getListFromServerOrLocal(shoppingList.getListID()));
                System.out.println("\nList Actions:");
                System.out.println("1- Add Item");
                System.out.println("2- Update Item");
                System.out.println("3- Exit");
                System.out.print("Enter your choice: ");
            }
        }, 0, 5000); // Run the task every 5 seconds
    }

    private static void handleAddItem(Scanner scanner, ShoppingList shoppingList) {

        // prompt user for valid (new) item name until he enters a valid one or exits by pressing 0
        String itemName;
        while (true) {
            System.out.print("Enter Item Name (0 to exit): ");
            itemName = scanner.next();
            if (itemName.equals("0")) {
                handleAccessList(scanner, shoppingList.getListID());
            }
            if (!shoppingList.getListItems().containsKey(itemName)) {
                break;
            } else {
                System.out.println("Item already exists. Please try again.");
            }
        }

        // prompt user for valid item value until he enters a valid one or exits by entering -1
        int itemValue;
        while (true) {
            System.out.print("Enter Item Value (-1 to exit): ");
            try {
                itemValue = scanner.nextInt();
                if (itemValue == -1) {
                    handleAccessList(scanner, shoppingList.getListID());
                }
                break;
            } catch (Exception e) {
                ShoppingInterface.displayInvalidChoice();
                scanner.next();
            }
        }

        // get list from server
        ShoppingList remoteList = getListFromServer(shoppingList.getListID());

        if (remoteList == null) {
            synchronizeListWithServer(shoppingList.getListID(), Serializer.serialize(shoppingList));
        } else {
            shoppingList = shoppingList.merge(remoteList.getListItems());
        }

        long counter = 1;
        // check if the item exists in the list
        if (shoppingList.getListItems().containsKey(itemName)) {
            counter = shoppingList.getListItems().get(itemName).getTimestamp() + 1;
        }

        // create CRDT for item
        CRDT<String> crdtItem = new CRDT<>(Integer.toString(itemValue), counter, clientUUID);
        shoppingList.addItem(itemName, crdtItem);

        // try updating the list in the database
        if (!db.write(shoppingList, "update")) {
            System.out.println("Item locally added successfully.");
        } else {
            System.out.println("Failed to add item.");
        }

        // serialize the shopping list
        byte[] serializedList = Serializer.serialize(shoppingList);

        // synchronize the list with the cloud
        boolean syncSuccess = updateListInCloud(shoppingList.getListID(), serializedList);

        // display synchronization status
        ShoppingInterface.displaySynchronizationStatus(syncSuccess);

        // back to this list's menu
        handleAccessList(scanner, shoppingList.getListID());
    }

    private static void handleUpdateItem(Scanner scanner, ShoppingList shoppingList) {

        // prompt user for valid (existing) item name until he enters a valid one or exits by pressing 0
        String itemName;
        while (true) {
            System.out.print("Enter Item Name (0 to exit): ");
            itemName = scanner.next();
            if (itemName.equals("0")) {
                handleAccessList(scanner, shoppingList.getListID());
            }
            if (shoppingList.getListItems().containsKey(itemName)) {
                break;
            } else {
                System.out.println("Item not found. Please try again.");
            }
        }

        // prompt user for valid item value until he enters a valid one or exits by entering -1
        int itemValue;
        while (true) {
            System.out.print("Enter Item Value (-1 to exit): ");
            try {
                itemValue = scanner.nextInt();
                if (itemValue == -1) {
                    handleAccessList(scanner, shoppingList.getListID());
                }
                break;
            } catch (Exception e) {
                ShoppingInterface.displayInvalidChoice();
                scanner.next();
            }
        }

        // get list from server
        ShoppingList remoteList = getListFromServer(shoppingList.getListID());

        if (remoteList == null) {
            synchronizeListWithServer(shoppingList.getListID(), Serializer.serialize(shoppingList));
        } else {
            shoppingList = shoppingList.merge(remoteList.getListItems());
        }

        CRDT<String> crdtItem = new CRDT<>(Integer.toString(itemValue), shoppingList.getListItems().get(itemName).getTimestamp() + 1, clientUUID);
        shoppingList.updateItem(itemName, crdtItem);

        // try updating the list in the database
        if (!db.write(shoppingList, "update")) {
            System.out.println("Item locally updated successfully.");
        } else {
            System.out.println("Failed to update item.");
        }

        // serialize the shopping list
        byte[] serializedList = Serializer.serialize(shoppingList);

        // synchronize the list with the cloud
        boolean syncSuccess = updateListInCloud(shoppingList.getListID(), serializedList);

        // display synchronization status
        ShoppingInterface.displayItemUpdateSuccess(syncSuccess);

        // back to this list's menu
        handleAccessList(scanner, shoppingList.getListID());
    }

    private static boolean updateListInCloud(String listID, byte[] serializedList) {
        try {
            // get the preference list
            List<String> preferenceList = ring.getPreferenceList(listID, 3);

            for (String server : preferenceList) {

                // create http client and request
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/update?uuid=" + listID + "&cord=true"))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(serializedList))
                        .build();

                try {
                    // send the request
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // check the response code
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

    private static ShoppingList getListFromServer(String listUUID) {
        try {
            // getting list from server
            System.out.println("\nAttempting to get list from cloud...");

            // get the preference list
            List<String> preferenceList = ring.getPreferenceList(listUUID, 3);

            // print the preference list
            ShoppingInterface.printShoppingList(preferenceList);

            // loop through the preference list and try to get the list from the cloud
            int serversDown = 0;
            for (String server : preferenceList) {

                // create http client and request
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + server + "/read?uuid=" + listUUID + "&cord=true"))
                        .GET()
                        .build();

                try {
                    // send the request
                    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                    // check the response code
                    if (response.statusCode() == 200) {
                        return Serializer.deserialize(response.body());
                    } else if (response.statusCode() == 404) { // list not found in server
                        System.out.println("\nList not found in " + server + "!");
                        ShoppingList localList = db.read(listUUID); // get the list from local database
                        if (localList != null) { // if the list exists in local database then synchronize it with the server
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
