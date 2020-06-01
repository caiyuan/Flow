package me.caiyuan.flow;

import me.caiyuan.flow.xml.XML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan
 */
public class FlowArgument extends FlowConstructor {

    public FlowArgument(String id, FlowParameter param) {
        super(id, param);
    }

    /**
     * 读入argument配置和命令行参数,如果命令行和argument配置有相同参数则采用命令行为准
     */
    public Map<String, String> process() {
        Map<String, String> argument = new HashMap<>();
        Map<String, String> args = param.getArgs();

        Map<String, String> values = new HashMap<>();
        List<XML> argumentList = param.getTags("argument");
        if (argumentList != null)
            for (XML xml : argumentList) {
                String key = xml.getAttribute("key");
                String value = xml.getAttribute("value");
                if (key == null || value == null) continue;
                values.put(key, value);
            }

        argument.putAll(values);
        argument.putAll(args);
        return argument;
    }

}
