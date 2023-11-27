package org.WishCloud.Cloud;

import org.WishCloud.Database.SQl;

public class Server {
    private final int port;
    private volatile boolean isRunning = true;
    private final SQl database;

    public Server(int port) {
        this.port = port;
        this.database = new SQl("server_" + port + ".db");
        this.database.connect();
    }

    public void start() {
        try {
            // create http server
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up resources here (e.g., close channels, database connections, etc.)
            closeResources();
        }
    }

    public void stop() {
        isRunning = false;
    }

    private void closeResources() {
        // Add code here to close resources like channels, database connections, etc.
        try {
            // Close database connection
            this.database.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


