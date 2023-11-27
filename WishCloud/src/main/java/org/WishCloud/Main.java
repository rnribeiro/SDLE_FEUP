package org.WishCloud;

import org.WishCloud.Database.SQl;

public class Main {
    public static void main(String[] args) {

        SQl sql = new SQl("test.db");
        sql.connect();

        sql.insertList("test_1","test");
        sql.insertItem("item",4,"test_1");
        sql.deleteList("test_1");

        sql.close();
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