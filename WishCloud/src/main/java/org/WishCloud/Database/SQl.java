package org.WishCloud.Database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Map;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.ShoppingList.ShoppingList;

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

    public SQl(String dbName) {
        this.dbName = dbName;
    }

    public void createDB() {
        String path = System.getProperty("user.dir");
        Path fullPath = Paths.get(path, "DBs", this.dbName);
        String url = "jdbc:sqlite:" + fullPath;

        try {
            this.conn = DriverManager.getConnection(url);
            if (this.conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("Connected to database.");

                Statement stmt = conn.createStatement();
                stmt.execute(sql_lists);
                stmt.execute(sql_items);
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

    public synchronized void insertSL(ShoppingList list) {
        String uuid = list.getListID();
        String name = list.getName();
        Map<String, CRDT<String>> items = list.getListItems();

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
        } finally {
            close();
        }
    }
}
