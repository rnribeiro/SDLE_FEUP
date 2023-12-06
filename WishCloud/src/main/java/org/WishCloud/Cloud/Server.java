package org.WishCloud.Cloud;

import org.WishCloud.Database.SQl;
import org.WishCloud.Cloud.Handlers.CreateHandler;
import org.WishCloud.Cloud.Handlers.ReadHandler;
import org.WishCloud.Cloud.Handlers.SynchroniseHandler;
import org.WishCloud.Cloud.Handlers.DeleteHandler;
import org.WishCloud.Cloud.Handlers.RefreshHandler;

import com.sun.net.httpserver.HttpServer;
import org.WishCloud.Utils.Ring;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private static final int HashSpace = 1 << 31;
    private final String serverIp;
    private final int serverPort;
    private final String serverName;
    private final List<String> seeds;

    public Server(String ServerIp, int serverPort, List<String> seeds) {
        this.serverIp = ServerIp;
        this.serverPort = serverPort;
        this.serverName = ServerIp + ":" + serverPort;
        this.seeds = seeds;
    }

    public void start() {
        try {
            // create database
            SQl db = new SQl(this.serverIp + "_" + this.serverPort + ".db");
            db.createDB();

            // compute ring
            Ring ring = new Ring(HashSpace);
            for (String seed : this.seeds) { ring.addNode(seed, 10); }
            if (!this.seeds.contains(this.serverName)) { ring.addNode(this.serverName, 10); }

            // thread pool
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

            // create http server
            HttpServer server = HttpServer.create(new InetSocketAddress(this.serverIp, this.serverPort), 0);

            // create contexts
            server.createContext("/create", new CreateHandler());
            server.createContext("/synchronize", new SynchroniseHandler(ring, this.serverName, db));
            server.createContext("/read", new ReadHandler());
            server.createContext("/refresh", new RefreshHandler());

            // set executor
            server.setExecutor(threadPoolExecutor);

            // Start the server
            server.start();
            System.out.println("Server started on http://" + this.serverName);

            // Add a shutdown hook to stop the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down");
                server.stop(5); // Stop the server with a delay of 5 seconds
                System.out.println("Server stopped");
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // create server
        Server server = new Server("localhost", 8000, List.of("localhost:8000", "localhost:8001"));

        // start server
        server.start();
    }
}


