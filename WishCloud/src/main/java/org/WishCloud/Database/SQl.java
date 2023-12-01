package org.WishCloud.Database;

import java.sql.*;

public class SQl {
    private Connection conn = null;
    private final String dbName;
    private static final String sql_lists = """
            CREATE TABLE IF NOT EXISTS lists (
            	uuid text NOT NULL PRIMARY KEY,
            	name text NOT NULL
            );""";
    private static final String sql_items = """
            CREATE TABLE IF NOT EXISTS items (
            	name text NOT NULL,
            	marked integer NOT NULL,
            	list_uuid text NOT NULL,
            	FOREIGN KEY (list_uuid) REFERENCES lists (uuid)
            );""";

    public SQl(String dbName) {
        this.dbName = dbName;
    }

    public void connect() {
        String url = "jdbc:sqlite:C:/sqlite/db/" + this.dbName;

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
        }
    }

    public void insertList(String uuid, String name) {
        String sql = "INSERT INTO lists(uuid,name) VALUES(?,?)";

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(sql);
            pstmt.setString(1, uuid);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertItem(String name, int marked, String list_id) {
        String sql = "INSERT INTO items(name,marked,list_uuid) VALUES(?,?,?)";

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setInt(2, marked);
            pstmt.setString(3, list_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void updateList(String uuid, String name) {
        String sql = "UPDATE lists SET name = ? WHERE uuid = ?";

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void updateItem(String name, int marked, String list_id) {
        String sql = "UPDATE items SET marked = ? WHERE name = ? AND list_uuid = ?";

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(sql);
            pstmt.setInt(1, marked);
            pstmt.setString(2, name);
            pstmt.setString(3, list_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteList(String uuid) {
        String sql = "DELETE FROM lists WHERE uuid = ?";
        String sql2 = "DELETE FROM items WHERE list_uuid = ?";

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(sql);
            PreparedStatement pstmt2 = this.conn.prepareStatement(sql2);
            pstmt.setString(1, uuid);
            pstmt2.setString(1, uuid);
            pstmt.executeUpdate();
            pstmt2.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void close() {
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
