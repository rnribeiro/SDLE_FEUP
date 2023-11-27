package org.WishCloud.Client;

import java.util.UUID;

public class Client {

    public static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static void main(String[] args) {
        String randomUUID = generateUUID();
        System.out.println("Random UUID: " + randomUUID);
    }
}
