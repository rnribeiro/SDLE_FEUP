package org.WishCloud.Client.UI;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.Database.SQl;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Serializer;


public class ShoppingInterface {

    private static SQl db = new SQl("local_shopping.db");

    public static void main(String[] args) {
        db.connect();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMainMenu();
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    createList(scanner);
                    break;
                case 2:
                    accessList(scanner);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("Main Menu:");
        System.out.println("1- Create List");
        System.out.println("2- Access List");
        System.out.print("Enter your choice: ");
    }

    private static void createList(Scanner scanner) {
        System.out.print("Enter List Name: ");
        String listName = scanner.next();
        String listUUID = UUID.randomUUID().toString();

        ShoppingList shoppingList = new ShoppingList(listName, listUUID, new HashMap<>());
        db.insertSL(shoppingList);

        byte[] serializedList = Serializer.serialize(shoppingList);
        synchronizeListWithServer(listUUID, serializedList);

        System.out.println("List created successfully. UUID: " + listUUID);
    }

    private static void synchronizeListWithServer(String listUUID, byte[] serializedList) {
        try {
            URL url = new URL("http://" + SERVER_IP + ":" + SERVER_PORT + "/create");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.getOutputStream().write(serializedList);
    
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("List synchronized with server successfully.");
            } else {
                System.out.println("Failed to synchronize list with server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    

    private static void accessList(Scanner scanner) {
        System.out.print("Enter List UUID: ");
        String listUUID = scanner.next();

        ShoppingList shoppingList = getListFromServerOrLocal(listUUID);

        if (shoppingList != null && shoppingList.getListItems() != null && !shoppingList.getListItems().isEmpty()) {
            System.out.println("List Items:");
            System.out.println("------------------------------------------------------------");
            System.out.printf("%-20s | %-10s | %-10s | %-20s\n", "Item Name", "Value", "Counter", "Author");
            System.out.println("------------------------------------------------------------");

            for (Map.Entry<String, CRDT<String>> entry : shoppingList.getListItems().entrySet()) {
                String itemName = entry.getKey();
                CRDT<String> crdtItem = entry.getValue();
                int value = Integer.parseInt(crdtItem.getValue());
                long counter = crdtItem.getTimestamp();
                String author = crdtItem.getClientID();

                System.out.printf("%-20s | %-10d | %-10d | %-20s\n", itemName, value, counter, author);
            }
        } else {
            System.out.println("List not found or empty.");
        }
    }

    private static ShoppingList getListFromServerOrLocal(String listUUID) {
        // First try to get the list from the server
        ShoppingList shoppingList = getListFromServer(listUUID);

        if (shoppingList == null) {
            // If server is not responding, get the list from the local database
            db.connect();
            shoppingList = db.getShoppingList(listUUID);
            db.close();
        }

        return shoppingList;
    }

    private static ShoppingList getListFromServer(String listUUID) {
        try {
            // Após ter os servers mudar isto para os valores corretos
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
