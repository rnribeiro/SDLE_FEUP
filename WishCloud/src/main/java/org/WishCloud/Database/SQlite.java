package org.WishCloud.Database;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.CRDT.ShoppingList;
import org.sqlite.SQLiteConfig;

public abstract class SQlite {
    protected Connection conn = null;
    protected final String dbName;
    public SQlite(String name) {
        this.dbName = "db_" + name + ".db";
    }
    public abstract void createDB();

    protected void connect() {
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

    protected void close() {
        try {
            if (this.conn != null) {
                if (!this.conn.getAutoCommit()) { this.conn.rollback(); }
                this.conn.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    protected void beginTransaction() {
        try {
            if (this.conn != null) {
                this.conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    protected void commitTransaction() {
        try {
            if (this.conn != null) {
                this.conn.commit();
                this.conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    protected void rollbackTransaction() {
        try {
            if (this.conn != null) {
                if (!this.conn.getAutoCommit()) { this.conn.rollback(); }
                this.conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
