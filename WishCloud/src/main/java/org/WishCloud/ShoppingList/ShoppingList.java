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

}

