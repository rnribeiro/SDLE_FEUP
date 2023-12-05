import org.junit.jupiter.api.Test;
import org.WishCloud.CRDT.CRDT;
import org.WishCloud.ShoppingList.ShoppingList;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ShoppingListTest {

    @Test
    void testMerge() {
        Map<String, CRDT<String>> items1 = new HashMap<>();
        items1.put("apple", new CRDT<>("2", 123456789, "client1"));
        items1.put("orange", new CRDT<>("9", 123456790, "client1"));

        Map<String, CRDT<String>> items2 = new HashMap<>();
        items2.put("orange", new CRDT<>("3", 123456791, "client2"));
        items2.put("banana", new CRDT<>("4", 123456792, "client2"));

        ShoppingList list1 = new ShoppingList("list1","list1", items1);
        ShoppingList list2 = new ShoppingList("list2","list2", items2);

        Map<String, CRDT<String>> mergedItems = list1.merge(list2.getListItems());

        assertEquals("3", mergedItems.get("orange").getValue());
        assertEquals(123456791, mergedItems.get("orange").getTimestamp());
        assertEquals("client2", mergedItems.get("orange").getClientID());

        assertEquals("4", mergedItems.get("banana").getValue());
        assertEquals(123456792, mergedItems.get("banana").getTimestamp());
        assertEquals("client2", mergedItems.get("banana").getClientID());

        assertEquals("2", mergedItems.get("apple").getValue());
        assertEquals(123456789, mergedItems.get("apple").getTimestamp());
        assertEquals("client1", mergedItems.get("apple").getClientID());
    }

    @Test
    // test printShoppingList
    void testPrintShoppingList() {
        Map<String, CRDT<String>> items = new HashMap<>();
        items.put("apple", new CRDT<>("2", 123456789, "client1"));
        items.put("orange", new CRDT<>("9", 123456790, "client1"));

        ShoppingList list = new ShoppingList("list1","list1", items);

        list.printShoppingList();
    }
}
