package org.WishCloud.CRDT;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShoppingList {
    // declare variables
    private String listID;
    private String name;
    private Map<String, CRDT<String>> listItems;


    public ShoppingList(
            @JsonProperty("name")String name,
            @JsonProperty("listID")String listID,
            @JsonProperty("listItems")Map<String, CRDT<String>> listItems
    ) {
        this.name = name;
        this.listID = listID;
        this.listItems = listItems;
    }

    public String getListID() {
        return listID;
    }

    public void setListID(String listID) {
        this.listID = listID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, CRDT<String>> getListItems() {
        return listItems;
    }

    public void setListItems(Map<String, CRDT<String>> listItems) {
        this.listItems = listItems;
    }

    // toString
    @Override
    public String toString() {
        return "ShoppingList{" +
                "name='" + name + '\'' +
                "listID='" + listID + '\'' +
                ", listItems=" + listItems +
                '}';
    }

    // create map merge function and return a new shopping list object
    public ShoppingList merge(Map<String, CRDT<String>> other) {
        Map<String, CRDT<String>> mergedItems = new HashMap<>();

        for (Map.Entry<String, CRDT<String>> entry : this.listItems.entrySet()) {
            String itemName = entry.getKey();
            CRDT<String> crdtItem = entry.getValue();

            if (other.containsKey(itemName)) {
                CRDT<String> otherCrdtItem = other.get(itemName);
                CRDT<String> mergedCrdtItem = crdtItem.merge(otherCrdtItem);
                mergedItems.put(itemName, mergedCrdtItem);
            } else {
                mergedItems.put(itemName, crdtItem);
            }
        }

        for (Map.Entry<String, CRDT<String>> entry : other.entrySet()) {
            String itemName = entry.getKey();
            CRDT<String> crdtItem = entry.getValue();

            if (!this.listItems.containsKey(itemName)) {
                mergedItems.put(itemName, crdtItem);
            }
        }

        return new ShoppingList(this.name, this.listID, mergedItems);
    }

    // loop through map and print shopping list
    public void printShoppingList() {
        System.out.println("List Items:");
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-20s | %-10s | %-10s | %-20s\n", "Item Name", "Value", "Counter", "Author");
        System.out.println("------------------------------------------------------------");

        for (Map.Entry<String, CRDT<String>> entry : this.listItems.entrySet()) {
            String itemName = entry.getKey();
            CRDT<String> crdtItem = entry.getValue();
            int value = Integer.parseInt(crdtItem.getValue());
            long counter = crdtItem.getTimestamp();
            String author = crdtItem.getClientID();

            System.out.printf("%-20s | %-10d | %-10d | %-20s\n", itemName, value, counter, author);
        }
    }

    // loop through map and convert shopping list to json with formatting
    public String toJson() {
        String json = "{\n";
        json += "\t\"name\": \"" + this.name + "\",\n";
        json += "\t\"listID\": \"" + this.listID + "\",\n";
        json += "\t\"listItems\": {\n";
        for (Map.Entry<String, CRDT<String>> entry : this.listItems.entrySet()) {
            String itemName = entry.getKey();
            CRDT<String> crdtItem = entry.getValue();
            int value = Integer.parseInt(crdtItem.getValue());
            long counter = crdtItem.getTimestamp();
            String author = crdtItem.getClientID();

            json += "\t\t\"" + itemName + "\": {\n";
            json += "\t\t\t\"value\": \"" + value + "\",\n";
            json += "\t\t\t\"timestamp\": \"" + counter + "\",\n";
            json += "\t\t\t\"clientID\": \"" + author + "\"\n";
            json += "\t\t},\n";
        }
        json = json.substring(0, json.length() - 2);
        json += "\n\t}\n";
        json += "}";
        return json;
    }


    // add item to shopping list
    public void addItem(String itemName, CRDT<String> crdtItem) {
        this.listItems.put(itemName, crdtItem);
    }

    // remove item from shopping list
    public void removeItem(String itemName) {
        this.listItems.remove(itemName);
    }

    // update item in shopping list (value)
    public void updateItem(String itemName, CRDT<String> crdtItem) {
        this.listItems.put(itemName, crdtItem);
    }

}

