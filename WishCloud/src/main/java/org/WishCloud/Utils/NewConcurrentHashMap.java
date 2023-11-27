package org.WishCloud.Utils;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class NewConcurrentHashMap<K,V> {
    private final int numSegments = 16;
    private final List<Segment<K, V>> segments;

    public NewConcurrentHashMap() {
        segments = new ArrayList<>();
        for (int i = 0; i < numSegments; i++) {
            segments.add(new Segment<>());
        }
    }

    public void put(K key, V value) {
        int segmentIndex = getSegmentIndex(key);
        segments.get(segmentIndex).put(key, value);
    }

    public V get(K key) {
        int segmentIndex = getSegmentIndex(key);
        return segments.get(segmentIndex).get(key);
    }

    public void remove(K key) {
        int segmentIndex = getSegmentIndex(key);
        segments.get(segmentIndex).remove(key);
    }

    public Set<K> keySet() {
        Set<K> keySet = new HashSet<>();
        for (Segment<K,V> segment : segments) {
            keySet.addAll(segment.map.keySet());
        }
        return keySet;
    }

    private int getSegmentIndex(K key) {
        return Math.abs(key.hashCode()) % numSegments;
    }

    public boolean containsKey(K Key) {
        int segmentIndex = getSegmentIndex(Key);
        return segments.get(segmentIndex).map.containsKey(Key);
    }

    private static class Segment<K, V> {
        private final Map<K, V> map;
        private final ReentrantLock lock;

        public Segment() {
            map = new HashMap<>();
            lock = new ReentrantLock();
        }

        public void put(K key, V value) {
            lock.lock();
            try {
                map.put(key, value);
            } finally {
                lock.unlock();
            }
        }

        public V get(K key) {
            lock.lock();
            try {
                return map.get(key);
            } finally {
                lock.unlock();
            }
        }

        public void remove(K key) {
            lock.lock();
            try {
                map.remove(key);
            } finally {
                lock.unlock();
            }
        }

        public Set<K> keySet() {
            return map.keySet();
        }
    }
}

