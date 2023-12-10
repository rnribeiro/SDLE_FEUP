package org.WishCloud.Database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.CRDT.ShoppingList;
import org.sqlite.SQLiteConfig;

public class SQl {
    private Connection conn = null;
    private final String dbName;
    private static final String sql_lists = """
            CREATE TABLE IF NOT EXISTS lists (
                name text NOT NULL,
            	uuid text NOT NULL PRIMARY KEY
            );""";
    private static final String sql_items = """
            CREATE TABLE IF NOT EXISTS items (
            	name text NOT NULL,
            	value integer NOT NULL,
            	counter integer NOT NULL,
            	author text NOT NULL,
            	list_uuid text NOT NULL,
            	FOREIGN KEY (list_uuid) REFERENCES lists (uuid)
            );""";


    public SQl(String name) {
        this.dbName = "db_" + name + ".db";
    }

    public void createDB() {
        String path = System.getProperty("user.dir");
        Path fullPath = Paths.get(path, "DBs", this.dbName);
        if (fullPath.getParent().toFile().mkdirs()) {
            System.out.println("Created DBs directory.");
        }
        String url = "jdbc:sqlite:" + fullPath;


        try {
            this.conn = DriverManager.getConnection(url);
            if (this.conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                // System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("Connected to database.");

                Statement stmt = conn.createStatement();
                stmt.execute(sql_lists);
                stmt.execute(sql_items);
//                stmt.execute(sql_hinted_nodes);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            close();
        }
    }

    private void connect() {
        String path = System.getProperty("user.dir");
        Path fullPath = Paths.get(path, "DBs", this.dbName);
        String url = "jdbc:sqlite:" + fullPath;

        try {
            this.conn = DriverManager.getConnection(url);
            SQLiteConfig config = new SQLiteConfig();
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.apply(this.conn);
            System.out.println("Connected to database.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void close() {
        try {
            if (this.conn != null) {
                if (!this.conn.getAutoCommit()) { this.conn.rollback(); }
                this.conn.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void beginTransaction() {
        try {
            if (this.conn != null) {
                this.conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void commitTransaction() {
        try {
            if (this.conn != null) {
                this.conn.commit();
                this.conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void rollbackTransaction() {
        try {
            if (this.conn != null) {
                if (!this.conn.getAutoCommit()) { this.conn.rollback(); }
                this.conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean insertShoppingList(ShoppingList list) {
        String uuid = list.getListID();
        String name = list.getName();
        Map<String, CRDT<String>> items = list.getListItems();
        boolean error = false;

        connect();
        beginTransaction();
        try {
            // insert list
            String sql = "INSERT INTO lists (name, uuid) VALUES (?, ?)";
            PreparedStatement pstmt = this.conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();

            // insert items
            for (Map.Entry<String, CRDT<String>> entry : items.entrySet()) {
                String item_name = entry.getKey();
                int value = Integer.parseInt(entry.getValue().getValue());
                long counter = entry.getValue().getTimestamp();
                String author = entry.getValue().getClientID();

                sql = "INSERT INTO items (name, value, counter, author, list_uuid) VALUES (?, ?, ?, ?, ?)";
                pstmt = this.conn.prepareStatement(sql);
                pstmt.setString(1, item_name);
                pstmt.setInt(2, value);
                pstmt.setLong(3, counter);
                pstmt.setString(4, author);
                pstmt.setString(5, uuid);
                pstmt.executeUpdate();
            }

            commitTransaction();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            rollbackTransaction();
            error = true;
        } finally {
            close();
        }

        return error;
    }

    private boolean updateShoppingList(ShoppingList updatedList) {
        connect();
        beginTransaction();
        boolean error = false;

        try {
            String uuid = updatedList.getListID();
            String name = updatedList.getName();
            Map<String, CRDT<String>> items = updatedList.getListItems();

            // Update the list name in the 'lists' table
            String updateListSql = "UPDATE lists SET name = ? WHERE uuid = ?";
            PreparedStatement updateListPstmt = this.conn.prepareStatement(updateListSql);
            updateListPstmt.setString(1, name);
            updateListPstmt.setString(2, uuid);
            updateListPstmt.executeUpdate();

            // Clear existing items for the list in the 'items' table
            String deleteItemsSql = "DELETE FROM items WHERE list_uuid = ?";
            PreparedStatement deleteItemsPstmt = this.conn.prepareStatement(deleteItemsSql);
            deleteItemsPstmt.setString(1, uuid);
            deleteItemsPstmt.executeUpdate();

            // Insert updated items into the 'items' table
            String insertItemsSql = "INSERT INTO items (name, value, counter, author, list_uuid) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement insertItemsPstmt = this.conn.prepareStatement(insertItemsSql);
            for (Map.Entry<String, CRDT<String>> entry : items.entrySet()) {
                String itemName = entry.getKey();
                int value = Integer.parseInt(entry.getValue().getValue());
                long counter = entry.getValue().getTimestamp();
                String author = entry.getValue().getClientID();

                insertItemsPstmt.setString(1, itemName);
                insertItemsPstmt.setInt(2, value);
                insertItemsPstmt.setLong(3, counter);
                insertItemsPstmt.setString(4, author);
                insertItemsPstmt.setString(5, uuid);
                insertItemsPstmt.executeUpdate();
            }

            commitTransaction();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            error = true;
            rollbackTransaction();
        } finally {
            close();
        }

        return error;
    }

    private boolean deleteShoppingList(String listUUID) {
        connect();
        beginTransaction();
        boolean error = false;

        try {
            // Delete the list from the 'lists' table
            String deleteListSql = "DELETE FROM lists WHERE uuid = ?";
            PreparedStatement deleteListPstmt = this.conn.prepareStatement(deleteListSql);
            deleteListPstmt.setString(1, listUUID);
            deleteListPstmt.executeUpdate();

            // Delete the items from the 'items' table
            String deleteItemsSql = "DELETE FROM items WHERE list_uuid = ?";
            PreparedStatement deleteItemsPstmt = this.conn.prepareStatement(deleteItemsSql);
            deleteItemsPstmt.setString(1, listUUID);
            deleteItemsPstmt.executeUpdate();

            // delete the list from the 'hinted_nodes' table
            String deleteHintedNodeSql = "DELETE FROM hinted_nodes WHERE list_uuid = ?";
            PreparedStatement deleteHintedNodePstmt = this.conn.prepareStatement(deleteHintedNodeSql);
            deleteHintedNodePstmt.setString(1, listUUID);
            deleteHintedNodePstmt.executeUpdate();

            commitTransaction();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            error = true;
            rollbackTransaction();
        } finally {
            close();
        }

        return error;
    }

    public synchronized boolean write(ShoppingList list, String method) {
        return switch (method) {
            case "insert" -> insertShoppingList(list);
            case "update" -> updateShoppingList(list);
            case "delete" -> deleteShoppingList(list.getListID());
            default -> true;
        };
    }

    public ShoppingList read(String listUUID) {
        ShoppingList shoppingList = null;

        connect();
        try {
            // Query to get the shopping list details
            String listSql = "SELECT * FROM lists WHERE uuid = ?";
            PreparedStatement listPstmt = this.conn.prepareStatement(listSql);
            listPstmt.setString(1, listUUID);
            ResultSet listRs = listPstmt.executeQuery();

            if (listRs.next()) {
                String name = listRs.getString("name");

                // Query to get items of the shopping list
                String itemsSql = "SELECT * FROM items WHERE list_uuid = ?";
                PreparedStatement itemsPstmt = this.conn.prepareStatement(itemsSql);
                itemsPstmt.setString(1, listUUID);
                ResultSet itemsRs = itemsPstmt.executeQuery();

                Map<String, CRDT<String>> items = new HashMap<>();
                while (itemsRs.next()) {
                    String itemName = itemsRs.getString("name");
                    String value = String.valueOf(itemsRs.getInt("value"));
                    long counter = itemsRs.getLong("counter");
                    String author = itemsRs.getString("author");

                    CRDT<String> crdtItem = new CRDT<>(value, counter, author);
                    items.put(itemName, crdtItem);
                }

                shoppingList = new ShoppingList(name, listUUID, items);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            close();
        }

        return shoppingList;
    }
}
