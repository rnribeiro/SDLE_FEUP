package org.WishCloud.CRDT;

public class CRDT<T> {
    private T value;
    private long timestamp;
    private String clientID;

    // constructor
    public CRDT(T value, long timestamp, String clientID) {
        this.value = value;
        this.timestamp = timestamp;
        this.clientID = clientID;
    }

    // create merge function
    public CRDT<T> merge(CRDT<T> other) {
        if (other.timestamp > this.timestamp) {
            return other;
//            this.value = other.value;
//            this.timestamp = other.timestamp;
//            this.clientID = other.clientID;
        } else if (other.timestamp == this.timestamp) {
            if (other.clientID.compareTo(this.clientID) > 0) {
                return other;
//                this.value = other.value;
//                this.clientID = other.clientID;
            }
        }
        return this;
    }

    // getters and setters
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // toString
    @Override
    public String toString() {
        return "CRDT current state: {value=" + value + ", timestamp=" + timestamp + ", clientID=" + clientID + "}";
    }

}
