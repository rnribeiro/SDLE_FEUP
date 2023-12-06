package org.WishCloud.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.binarySearch;

public class Ring {
    private final List<Long> hashKeys;
    private final HashMap<Long, String> hashNodes;
    private final List<String> nodes;
    private final MessageDigest hashFunction;
    private final int HashSpace;

    public Ring(int HashSpace) {
        this.hashKeys = new ArrayList<>();
        this.hashNodes = new HashMap<>();
        this.nodes = new ArrayList<>();
        this.HashSpace = HashSpace;

        try {
            this.hashFunction = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Long generateHash(String key) {
        hashFunction.reset();
        hashFunction.update(key.getBytes());
        byte[] digest = hashFunction.digest();
        long hash = 0;
        for (int i = 0; i < 4; i++) {
            hash <<= 8;
            hash |= ((int) digest[i]) & 0xFF;
        }

        return hash;
    }

    public void addNode(String node, int replicas) {
        for (int i = 0; i < replicas; i++) {
            String virtualNode = node + i;
            Long hash = generateHash(virtualNode) % HashSpace;
            hashKeys.add(hash);
            hashNodes.put(hash, node);
        }
        nodes.add(node);
        hashKeys.sort(Long::compareTo);
    }

    public void removeNode(String node, int replicas) {
        for (int i = 0; i < replicas; i++) {
            String virtualNode = node + i;
            Long hash = generateHash(virtualNode) % HashSpace;
            hashKeys.remove(hash);
            hashNodes.remove(hash);
        }
        nodes.remove(node);
        hashKeys.sort(Long::compareTo);
    }

    public String getNode(String key) {
        if (hashKeys.isEmpty()) {
            return null;
        }

        Long hash = generateHash(key) % HashSpace;
        int index = binarySearch(hashKeys, hash);
        if (index < 0) {
            index = -index - 1;
            if (index >= hashKeys.size()) {
                index = 0;
            }
        }
        return hashNodes.get(hashKeys.get(index));
    }

    public List<String> getNodes() {
        return nodes;
    }

    public List<String> getNextNodes(String currentNode, int count) {
        List<String> result = new ArrayList<>();
        int currentIndex = nodes.indexOf(currentNode);

        if (currentIndex != -1) {
            int size = nodes.size();
            for (int i = 1; i <= count; i++) {
                int nextIndex = (currentIndex + i) % size;
                result.add(nodes.get(nextIndex));
            }
        }

        return result;
    }

}
