package org.WishCloud.ShoppingList;

import java.util.Map;
import org.WishCloud.CRDT.CRDT;

public class ShoppingList {
    // declare variables
    private String listID;
    private Map<String, CRDT<String>> listItems;

    // constructor
    public ShoppingList(String listID, Map<String, CRDT<String>> listItems) {
        this.listID = listID;
        this.listItems = listItems;
    }

    // getters and setters
    public String getListID() {
        return listID;
    }

    public void setListID(String listID) {
        this.listID = listID;
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
                "listID='" + listID + '\'' +
                ", listItems=" + listItems +
                '}';
    }

}

