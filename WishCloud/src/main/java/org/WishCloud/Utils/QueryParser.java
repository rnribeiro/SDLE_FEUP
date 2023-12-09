package org.WishCloud.Utils;

import java.util.HashMap;
import java.util.Map;

public class QueryParser {
    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length > 1) {
                    result.put(keyValue[0], keyValue[1]);
                } else {
                    result.put(keyValue[0], null);
                }
            }
        }
        return result;
    }
}
