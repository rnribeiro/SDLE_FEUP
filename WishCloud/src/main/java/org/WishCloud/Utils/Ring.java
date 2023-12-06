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

    /**
     * Returns the index of the virtual node in the hashKeys list
     * Use to map item to the virtual node
     * @param key the key to be hashed
     * @return index of the virtual node in the hashKeys() list
     */
    public int getVNIndex(String key) {
        if (hashKeys.isEmpty()) { return -1; }

        long hash = generateHash(key) % HashSpace;
        int index = binarySearch(hashKeys, hash);
        if (index < 0) {
            index = -index - 1;
            if (index >= hashKeys.size()) { index = 0; }
        }
        return index;
    }

    /**
     * Returns the physical node that the key is mapped to
     * @param key the key to be hashed
     * @return the node that the key is mapped to
     */
    public String getNode(String key) {
        if (hashKeys.isEmpty() || (getVNIndex(key) == -1)) { return null; }
        return hashNodes.get(hashKeys.get(getVNIndex(key)));
    }

    /**
     * Returns list of physical nodes by order of preference
     * Is determined by the order of the virtual nodes
     * Skips nodes that map to the same physical node
     * List made of every physical node in the ring
     * @param Key the key to be hashed
     * @return the node that the key is mapped to
     */
    public List<String> getPreferenceList(String Key) {
        List<String> preferenceList = new ArrayList<>();
        int vNodeIndex;
        if (hashKeys.isEmpty() || (vNodeIndex = getVNIndex(Key)) == -1) { return null; }

        int count = 0;
        while (count < nodes.size()) {
            String node = hashNodes.get(hashKeys.get(vNodeIndex));
            if (!preferenceList.contains(node)) {
                preferenceList.add(node);
                count++;
            }
            vNodeIndex = (vNodeIndex + 1) % hashKeys.size();
        }

        return preferenceList;
    }

    /**
     * Returns preference list for a specific item
     * @param Key the key to be hashed
     * @param count the number of nodes to return
     * @return the node that the key is mapped to
     */
    public List<String> getPreferenceList(String Key, int count) {
        List<String> preferenceList = getPreferenceList(Key);
        preferenceList = preferenceList.subList(0, count);

        return preferenceList;
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
