package me.caiyuan.flow.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ryan
 */
public class FlowUtil {

    public static Map<String, String> map(String[] args) {
        Map<String, String> result = new HashMap<>();
        String key = "";
        StringBuilder value = new StringBuilder();
        for (String arg : args) {
            if (arg.indexOf("-") == 0) {
                result.put(key, value.toString());
                key = arg.replaceFirst("-", "");
                value = new StringBuilder();
            } else {
                value.append(value.toString().equals("") ? "" : " ").append(arg);
            }
        }
        result.put(key, value.toString());
        result.remove("");
        return result;
    }

}
