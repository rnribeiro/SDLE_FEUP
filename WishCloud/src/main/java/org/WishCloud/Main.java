package org.WishCloud;

import org.WishCloud.CRDT.ShoppingList;
import org.WishCloud.CRDT.CRDT;
import org.WishCloud.Database.Storage;

import java.util.Map;

public class Main {
    public static void main(String[] args) {

        Storage db = new Storage("test");
        db.createDB();

        // create shopping list
        Map<String,CRDT<String>> listItems = Map.of(
                "item1", new CRDT<>("1", 1, "client1"),
                "item2", new CRDT<>("1", 2, "client2"),
                "item3", new CRDT<>("0", 3, "client3")
        );
        ShoppingList shoppingList = new ShoppingList("test", "test", listItems);

        System.out.println(db.write(shoppingList, "insert"));
        /*
        client
            create list id - done
            sqlite database setup - new class and call in client
                create database
                create tables
                CRUD
            connect to server

            send list to server

        server
            sqlite database setup - new class and call in server
                create database
                create tables
                CRUD

            seeds
                create seeds

        partitions
            create partitions
            define hash function
            define replication factor



         */
    }



}