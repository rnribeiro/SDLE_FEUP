package org.WishCloud.Cloud;

import org.WishCloud.Cloud.Handlers.*;
import org.WishCloud.Database.Backup;
import org.WishCloud.Database.Storage;

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
            Storage db = new Storage(this.serverIp + "_" + this.serverPort);
            db.createDB();
            Backup db_backup = new Backup(this.serverIp + "_" + this.serverPort + "_hinted");
            db_backup.createDB();

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
            server.createContext("/upload", new UploadHandler(this.serverName, ring, db, db_backup));
            server.createContext("/read", new ReadHandler(this.serverName, ring, db, db_backup));

            // set executor
            server.setExecutor(threadPoolExecutor);

            // Start the server
            server.start();
            HintedSender hintedSender = new HintedSender(this.serverName, db_backup);
            System.out.println("Server started on http://" + this.serverName);

            // add thread to periodically send hinted data

            // Add a shutdown hook to stop the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                hintedSender.stopThread();
                System.out.println(this.serverName + " shutting down");
                server.stop(5); // Stop the server with a delay of 5 seconds
                System.out.println(this.serverName + " stopped");
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


