package org.WishCloud.Client.UI;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.ShoppingList.ShoppingList;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ShoppingInterface {

    public static void printMainMenu() {
        System.out.println("\nMain Menu:");
        System.out.println("1- Create List");
        System.out.println("2- Access List");
        System.out.print("Enter your choice: ");
    }

    public static String promptForListName(Scanner scanner) {
        clearConsole();
        System.out.print("Enter List Name: ");
        return scanner.nextLine();
    }

    public static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String promptForListUUID(Scanner scanner) {
        System.out.print("Enter List UUID: ");
        return scanner.next();
    }

    public static void displayCreationSuccess(String listUUID, boolean error) {
        if (!error) {
            System.out.println("List created successfully locally. UUID: " + listUUID);
        } else {
            System.out.println("Failed to create list locally.");
        }
    }

    public static void displaySynchronizationStatus(boolean success) {
        if (success) {
            System.out.println("List synchronized with cloud successfully.");
        } else {
            System.out.println("Failed to synchronize list with cloud.");
        }
    }

    public static void displayInvalidChoice() {
        clearConsole();
        System.out.println("Invalid choice. Please try again.");
    }

    public static void displayListNotFound() {
        clearConsole();
        System.out.println("List not found!");
    }

    public static void displayShoppingList(ShoppingList shoppingList) {
        System.out.println("List Name: " + shoppingList.getName());
        System.out.println("List Items:");
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-20s | %-10s | %-10s | %-20s\n", "Item Name", "Value", "Counter", "Author");
        System.out.println("------------------------------------------------------------");

        if (shoppingList.getListItems().isEmpty()) {
            System.out.println("List is empty.\n\n");
            return;
        }

        for (Map.Entry<String, CRDT<String>> entry : shoppingList.getListItems().entrySet()) {
            String itemName = entry.getKey();
            CRDT<String> crdtItem = entry.getValue();
            int value = Integer.parseInt(crdtItem.getValue());
            long counter = crdtItem.getTimestamp();
            String author = crdtItem.getClientID();

            System.out.printf("%-20s | %-10d | %-10d | %-20s\n", itemName, value, counter, author);
        }
    }

    public static void displayItemUpdateSuccess(boolean syncSuccess) {
        if (syncSuccess) {
            System.out.println("Item updated successfully in cloud.");
        } else {
            System.out.println("Item updated locally. Failed to synchronize with cloud.");
        }
    }

    public static void printSyncAttempt() {
        System.out.println("\nAttempting to synchronize list with cloud...");
    }

    public static void printShoppingList(List<String> preferenceList) {
        // print the preference list
        System.out.println("\nPreference List:");
        for (String server : preferenceList) {
            System.out.println(server);
        }
    }

    public static void printListActions() {
        System.out.println("\nList Actions:");
        System.out.println("1- Add Item");
        System.out.println("2- Update Item");
        System.out.println("3- Exit");
        System.out.println("4- Refresh");
        System.out.print("Enter your choice: ");
    }


}
