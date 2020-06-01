package me.caiyuan.flow.xml;

import java.util.*;

/**
 * @author Ryan
 */
public class XML {

    private final Map<String, String> attributes = new HashMap<>();
    private final List<XML> childes = new ArrayList<>();
    private Map<String, List<XML>> index;
    private String node = null;
    private String text = null;
    private XML father = null;

    public XML() {
    }

    public XML(String node) {
        this.node = node;
    }

    // getter

    public String getNode() {
        return node;
    }

    void setNode(String node) {
        this.node = node;
    }

    public String getText() {
        return text;
    }

    void setText(String text) {
        this.text = "".equals(text) ? null : text;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    // setter

    public List<XML> getChildes() {
        return childes;
    }

    public XML getFather() {
        return father;
    }

    void setFather(XML father) {
        this.father = father;
    }

    void putAttribute(String key, String value) {
        String v = "".equals(value) ? null : value;
        this.attributes.put(key, v);
    }

    public void addChild(XML child) {
        this.childes.add(child);
    }

    // building index

    /**
     * @return 构造数据索引
     */
    public Map<String, List<XML>> index() {
        return index(false);
    }

    /**
     * @param again true: 重新构造数据索引
     */
    public Map<String, List<XML>> index(boolean again) {
        if (index == null || again) {
            index = new HashMap<>();
            build(this);
        }
        return index;
    }

    private void build(Object o) {
        if (o instanceof XML) {
            XML node = (XML) o;
            String key = node.getNode();
            List<XML> value = index.get(key);
            if (value == null) {
                List<XML> item = new ArrayList<>();
                index.put(key, item);
                item.add(node);
            } else {
                value.add(node);
            }
            List childes = node.getChildes();
            for (Object child : childes) {
                build(child);
            }
        } else if (o instanceof List) {
            for (Object item : (List) o) {
                build(item);
            }
        }
    }

    // toJSON

    @Override
    public String toString() {
        return "{" +
                " \"node\":\"" + node + "\"," +
                " \"text\":\"" + text + "\"," +
                " \"attributes\":" + toJSON(attributes) + "," +
                " \"childes\":" + childes +
                "}";
    }

    private String toJSON(Map<String, String> attr) {
        StringBuilder json = new StringBuilder("[");
        Iterator<String> keys = attr.keySet().iterator();
        for (int i = attr.size(); i > 0; i--) {
            String key = keys.next();
            String value = attr.get(key);
            json.append("{\"").append(key).append("\":\"").append(value).append("\"}");
            if (i > 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

}
