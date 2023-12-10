package org.WishCloud.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.WishCloud.CRDT.ShoppingList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Serializer {
    static public byte[] serialize(ShoppingList sl) {
        ObjectMapper mapper = new ObjectMapper();
        String message = null;
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = null;

        try {
            // convert object to json string
            message = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sl);

            // compress json string
            gzip = new GZIPOutputStream(obj);
            gzip.write(message.getBytes(StandardCharsets.UTF_8));
            gzip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj.toByteArray();
    }

    static public ShoppingList deserialize(byte[] message) {
        ObjectMapper mapper = new ObjectMapper();
        ShoppingList sl = null;
        GZIPInputStream gzip = null;
        BufferedReader br = null;

        try {
            // decompress json string
            gzip = new GZIPInputStream(new ByteArrayInputStream(message));
            br = new BufferedReader(new java.io.InputStreamReader(gzip, StandardCharsets.UTF_8));
            StringBuilder aux = new StringBuilder();
            String line;
            while ((line=br.readLine())!=null) { aux.append(line); }

            // convert json string to object
            sl = mapper.readValue(aux.toString(), ShoppingList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sl;
    }
}
