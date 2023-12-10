package org.WishCloud.Cloud;

import org.WishCloud.Cloud.Handlers.*;
import org.WishCloud.Database.SQl;

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
            SQl db = new SQl(this.serverIp + "_" + this.serverPort);
            db.createDB();

            // compute ring
            Ring ring = new Ring(HashSpace);
            for (String seed : this.seeds) {
                ring.addNode(seed, 10);
            }


            if (!this.seeds.contains(this.serverName)) {
                ring.addNode(this.serverName, 10);
            }

            // thread pool
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

            // create http server
            HttpServer server = HttpServer.create(new InetSocketAddress(this.serverIp, this.serverPort), 0);

            // create contexts
            server.createContext("/create", new CreateHandler(this.serverName, ring, db));
            server.createContext("/read", new ReadHandler(this.serverName, ring, db));
            server.createContext("/update", new UpdateHandler(this.serverName, ring, db));
            server.createContext("/delete", new DeleteHandler(this.serverName, ring, db));
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
}


