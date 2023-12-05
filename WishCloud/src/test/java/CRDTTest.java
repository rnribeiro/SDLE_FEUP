import org.junit.jupiter.api.Test;
import org.WishCloud.CRDT.CRDT;
import static org.junit.jupiter.api.Assertions.*;

class CRDTTest {

    @Test
    void testMerge() {
        CRDT<String> crdt1 = new CRDT<>("2", 5, "client1");
        CRDT<String> crdt2 = new CRDT<>("4", 4, "client2");

        CRDT<String> merged = crdt1.merge(crdt2);

        assertEquals("2", merged.getValue());
        assertEquals(5, merged.getTimestamp());
        assertEquals("client1", merged.getClientID());
    }

    @Test
    void testMergeEqualTimestamp() {
        CRDT<String> crdt1 = new CRDT<>("3", 123456789, "client1");
        CRDT<String> crdt2 = new CRDT<>("7", 123456789, "client2");

        CRDT<String> merged = crdt1.merge(crdt2);

        assertEquals("7", merged.getValue());
        assertEquals(123456789, merged.getTimestamp());
        assertEquals("client2", merged.getClientID());
    }

    @Test
    // test toJSON
    void testToJson() {
        CRDT<String> crdt = new CRDT<>("1", 1, "client1");
        String json = crdt.toJson();

        assertEquals("{\"value\":\"1\", \"timestamp\":1, \"clientID\":\"client1\"}", json);
    }

    @Test
    // test toString
    void testToString() {
        CRDT<String> crdt = new CRDT<>("1", 1, "client1");
        String str = crdt.toString();

        assertEquals("CRDT current state: {value=1, timestamp=1, clientID=client1}", str);
    }

    @Test
    // test fromJson
    void testFromJson() {
        String json = "{\"value\":1, \"timestamp\":2, \"clientID\":\"client3\"}";
        CRDT<String> crdt = new CRDT<>(json);

        System.out.println(crdt.toJson());

        assertEquals("1", crdt.getValue());
        assertEquals(2, crdt.getTimestamp());
        assertEquals("client3", crdt.getClientID());
    }
}
