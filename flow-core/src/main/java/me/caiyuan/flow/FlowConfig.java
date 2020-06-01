package me.caiyuan.flow;

import me.caiyuan.flow.xml.XML;
import me.caiyuan.flow.xml.XMLParse;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author Ryan
 * @see FlowConfig#validate(java.util.Map)
 */
public class FlowConfig {

    private static final Logger logger = Logger.getLogger(FlowConfig.class);
    private final String id;
    private final String clz;
    private final Set<String> configList;
    private final Set<String> pluginList;
    private final Map<String, String> beanMap;
    private final Type type;
    private boolean lazy;

    private FlowConfig(String id, String clz, boolean lazy, Set<String> configList, Set<String> pluginList, Map<String, String> beanMap, Type type) {
        this.id = id;
        this.clz = clz;
        this.lazy = lazy;
        this.configList = configList;
        this.pluginList = pluginList;
        this.beanMap = beanMap;
        this.type = type;
    }

    public static Map<Type, List<FlowConfig>> parse(String applicationConfigPath) throws Exception {
        Map<String, FlowConfig> configItems = parser(applicationConfigPath);
        Map<String, FlowConfig> flowConfigMap = validate(configItems);
        return sortByDependent(flowConfigMap);
    }

    private static Map<Type, List<FlowConfig>> sortByDependent(Map<String, FlowConfig> flowConfigMap) {
        LinkedList<String> indexing = new LinkedList<>(flowConfigMap.keySet());

        List<FlowConfig> items = new ArrayList<>(flowConfigMap.values());
        for (int i = items.size() - 1; i > 0; i--) {
            items.add(items.get(i));
        }

        for (FlowConfig config : items) {
            String id = config.id;
            List<String> list = new ArrayList<>();
            list.addAll(config.pluginList);
            list.addAll(config.beanMap.values());

            indexing.remove(id);
            int index = -1;
            for (String item : list) {
                int num = indexing.indexOf(item);
                if (index < num) index = num;
            }
            index = index + 1;
            indexing.add(index, id);
        }

        List<FlowConfig> valueConfigs = new LinkedList<>();
        List<FlowConfig> beanConfigs = new LinkedList<>();
        List<FlowConfig> tfConfigs = new LinkedList<>();

        for (String id : indexing) {
            FlowConfig config = flowConfigMap.get(id);
            if (Type.Flow.equals(config.type)) tfConfigs.add(0, config);
            if (Type.Bean.equals(config.type)) beanConfigs.add(config);
            if (Type.Argument.equals(config.type)) valueConfigs.add(config);
        }

        Map<Type, List<FlowConfig>> flowConfigs = new HashMap<>();
        flowConfigs.put(Type.Argument, valueConfigs);
        flowConfigs.put(Type.Bean, beanConfigs);
        flowConfigs.put(Type.Flow, tfConfigs);

        return flowConfigs;
    }

    /**
     * <pre>
     * 1. plugin 定义只允许在 tf 组件之间
     * 2. bean 属性只允许为 bean 组件
     * 3. bean 不允许懒初始化
     * </pre>
     */
    private static Map<String, FlowConfig> validate(Map<String, FlowConfig> flowConfigMap) {
        for (FlowConfig config : flowConfigMap.values()) {
            Set<String> del = new HashSet<>();
            // plugin
            if (Type.Flow.equals(config.type)) {
                for (String name : config.pluginList) {
                    FlowConfig plugin = flowConfigMap.get(name);
                    if (plugin == null) {
                        throw new NullPointerException("未找到 " + config.id + " 组件的 " + name + " 组件.");
                    }
                    if (!Type.Flow.equals(plugin.type)) del.add(name);
                }
                if (del.size() > 0) {
                    logger.warn("Flow#" + config.id + " --> plugin" + del);
                    for (String name : del) config.pluginList.remove(name);
                }
            }
            if (Type.Bean.equals(config.type)) {
                if (config.pluginList.size() > 0) {
                    logger.warn("Bean#" + config.id + " --> plugin" + config.pluginList);
                    config.pluginList.clear();
                }
            }
            if (Type.Argument.equals(config.type)) {
                if (config.pluginList.size() > 0) {
                    logger.warn("Argument#" + config.id + " --> plugin" + config.pluginList);
                    config.pluginList.clear();
                }
            }
            // property
            del.clear();
            for (String key : config.beanMap.keySet()) {
                String name = config.beanMap.get(key);
                FlowConfig property = flowConfigMap.get(name);
                if (property == null) {
                    throw new NullPointerException("未找到 " + config.id + " 组件的 " + name + " 组件.");
                }
                if (!Type.Bean.equals(property.type)) del.add(key);
            }
            if (del.size() > 0) {
                String type = "";
                if (Type.Flow.equals(config.type)) type = "Flow";
                if (Type.Bean.equals(config.type)) type = "Bean";
                if (Type.Argument.equals(config.type)) type = "Argument";
                logger.warn(type + "#" + config.id + " --> bean" + del);
                for (String key : del) config.beanMap.remove(key);
            }
            // lazy
            if (Type.Bean.equals(config.type) && config.lazy) {
                config.lazy = false;
                logger.warn("Bean#" + config.id + " --> lazy = true");
            }
            if (Type.Argument.equals(config.type) && config.lazy) {
                config.lazy = false;
                logger.warn("Argument#" + config.id + " --> lazy = true");
            }
        }
        return flowConfigMap;
    }

    private static Map<String, FlowConfig> parser(String applicationConfigPath) throws Exception {
        XML applicationConfig = XMLParse.parse(applicationConfigPath);
        Map<String, List<XML>> flowMap = applicationConfig.index();
        List<XML> applicationXml = new ArrayList<>();
        List<XML> tfList = flowMap.get("tf");
        if (tfList != null) applicationXml.addAll(tfList);
        List<XML> beanList = flowMap.get("bean");
        if (beanList != null) applicationXml.addAll(beanList);
        List<XML> valueList = flowMap.get("argument");
        if (valueList != null) applicationXml.addAll(valueList);

        Map<String, FlowConfig> configList = toFlowConfig(applicationXml);

        List<XML> resourceXml = flowMap.get("import");
        if (resourceXml != null) {
            for (XML resource : resourceXml) {
                String path = resource.getAttribute("resource");
                configList.putAll(parser(path));
            }
        }

        return configList;
    }

    private static Map<String, FlowConfig> toFlowConfig(List<XML> applicationXml) {
        Map<String, FlowConfig> flowConfigMap = new HashMap<>();
        if (applicationXml == null) {
            return flowConfigMap;
        }
        for (XML xml : applicationXml) {
            String id = xml.getAttribute("id");
            String clz = xml.getAttribute("class");
            String lazyStr = xml.getAttribute("lazy");
            boolean isLazy = "true".equals(lazyStr);
            String typeStr = xml.getNode();
            Type type = null;
            if (typeStr.equals(Type.Argument.toString())) type = Type.Argument;
            if (typeStr.equals(Type.Bean.toString())) type = Type.Bean;
            if (typeStr.equals(Type.Flow.toString())) type = Type.Flow;
            Map<String, List<XML>> childes = xml.index();
            Set<String> configList = list(childes.get("config"));
            Set<String> pluginList = list(childes.get("plugin"));
            Map<String, String> propertyMap = map(childes.get("property"));

            flowConfigMap.put(id, new FlowConfig(id, clz, isLazy, configList, pluginList, propertyMap, type));
        }
        return flowConfigMap;
    }

    private static Set<String> list(List<XML> xmlList) {
        Set<String> strList = new HashSet<>();
        if (xmlList != null)
            for (XML xml : xmlList) {
                String text = xml.getText();
                strList.add(text);
            }
        return strList;
    }

    private static Map<String, String> map(List<XML> property) {
        Map<String, String> propMap = new HashMap<>();
        if (property != null) {
            for (XML xml : property) {
                String key = xml.getAttribute("name");
                String value = xml.getText();
                propMap.put(key, value);
            }
        }
        return propMap;
    }

    // ========================================

    public String getId() {
        return id;
    }

    public String getClz() {
        return clz;
    }

    public boolean isLazy() {
        return lazy;
    }

    public Set<String> getConfigList() {
        return configList;
    }

    public Set<String> getPluginList() {
        return pluginList;
    }

    public Map<String, String> getBeanMap() {
        return beanMap;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        Argument, Bean, Flow;

        @Override
        public String toString() {
            switch (this) {
                case Argument:
                    return "argument";
                case Bean:
                    return "bean";
                case Flow:
                    return "tf";
                default:
                    return "undefined";
            }
        }
    }

}

