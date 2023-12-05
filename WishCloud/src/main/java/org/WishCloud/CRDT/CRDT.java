package org.WishCloud.CRDT;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CRDT<T> {
    private T value;
    private long timestamp;
    private String clientID;

    // constructor
    public CRDT(@JsonProperty("value")T value,
                @JsonProperty("timestamp")long timestamp,
                @JsonProperty("clientID")String clientID)
    {
        this.value = value;
        this.timestamp = timestamp;
        this.clientID = clientID;
    }

    // constructor from json
    public CRDT(String json) {
        String[] aux = json.split(",");
        this.value = (T) aux[0].split(":")[1];
        this.timestamp = Long.parseLong(aux[1].split(":")[1]);
        this.clientID = aux[2].split(":")[1].substring(1, aux[2].split(":")[1].length()-2);
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
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getClientID() {
        return this.clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    @Override
    public String toString() {
        return "CRDT current state: {value=" + value + ", timestamp=" + timestamp + ", clientID=" + clientID + "}";
    }

    // convert to json
    public String toJson() {
        return "{" +
                "\"value\": " + value +
                ", \"timestamp\": " + timestamp +
                ", \"clientID\": \"" + clientID + '\"' +
                '}';
    }
}
