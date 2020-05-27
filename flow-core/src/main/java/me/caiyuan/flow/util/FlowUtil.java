package me.caiyuan.flow.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ryan
 */
public class FlowUtil {

    public static Map<String, String> map(String[] args) {
        Map<String, String> result = new HashMap<String, String>();
        String key = "";
        String value = "";
        for (String arg : args) {
            if (arg.indexOf("-") == 0) {
                result.put(key, value);
                key = arg.replaceFirst("-", "");
                value = "";
            } else {
                value = value + (value.equals("") ? "" : " ") + arg;
            }
        }
        result.put(key, value);
        result.remove("");
        return result;
    }

}
