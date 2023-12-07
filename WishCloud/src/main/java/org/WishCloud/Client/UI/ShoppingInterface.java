package org.WishCloud.Client.UI;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.ShoppingList.ShoppingList;

import java.util.Map;
import java.util.Scanner;

public class ShoppingInterface {

    public static void printMainMenu() {
        System.out.println("Main Menu:");
        System.out.println("1- Create List");
        System.out.println("2- Access List");
        System.out.print("Enter your choice: ");
    }

    public static String promptForListName(Scanner scanner) {
        System.out.print("Enter List Name: ");
        return scanner.next();
    }

    public static String promptForListUUID(Scanner scanner) {
        System.out.print("Enter List UUID: ");
        return scanner.next();
    }

    public static void displayCreationSuccess(String listUUID) {
        System.out.println("List created successfully. UUID: " + listUUID);
    }

    public static void displaySynchronizationStatus(boolean success) {
        if (success) {
            System.out.println("List synchronized with server successfully.");
        } else {
            System.out.println("Failed to synchronize list with server.");
        }
    }

    public static void displayInvalidChoice() {
        System.out.println("Invalid choice. Please try again.");
    }

    public static void displayListNotFound() {
        System.out.println("List not found or empty.");
    }

    public static void displayShoppingList(ShoppingList shoppingList) {
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
    }
}
