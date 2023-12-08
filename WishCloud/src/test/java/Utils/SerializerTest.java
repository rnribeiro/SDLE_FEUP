package Utils;

import org.WishCloud.CRDT.CRDT;
import org.WishCloud.ShoppingList.ShoppingList;
import org.WishCloud.Utils.Serializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerializerTest {
    @Test
    void testSerializer() {
        Map<String,CRDT<String>> listItems = Map.of(
                "item1", new CRDT<>("1", 1, "client1"),
                "item2", new CRDT<>("1", 2, "client2"),
                "item3", new CRDT<>("0", 3, "client3")
        );
        ShoppingList shoppingList1 = new ShoppingList("test", "test", listItems);

        byte[] str = Serializer.serialize(shoppingList1);
        ShoppingList shoppingList2 = Serializer.deserialize(str);


        assertEquals(shoppingList1.toString(), shoppingList2.toString());
    }
}
