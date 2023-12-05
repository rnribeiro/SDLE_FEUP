package org.WishCloud.ShoppingList;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.WishCloud.CRDT.CRDT;

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

    // create map merge function
    public Map<String, CRDT<String>> merge(Map<String, CRDT<String>> other) {
        for (Map.Entry<String, CRDT<String>> entry : other.entrySet()) {
            if (this.listItems.containsKey(entry.getKey())) {
                this.listItems.put(entry.getKey(), this.listItems.get(entry.getKey()).merge(entry.getValue()));
            } else {
                this.listItems.put(entry.getKey(), entry.getValue());
            }
        }
        return this.listItems;
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

