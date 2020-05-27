package me.caiyuan.flow;

import me.caiyuan.flow.xml.XML;
import me.caiyuan.flow.xml.XMLParse;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan
 */
public class FlowParameter {

    protected XML xmlConfig = new XML("__root");
    protected Map<String, String> argument = new HashMap<String, String>();
    private Logger logger = Logger.getLogger(FlowParameter.class);
    private Map<String, List<XML>> xmlIndex = new HashMap<String, List<XML>>();

    public FlowParameter(Map<String, String> args) {
        if (args != null) this.argument.putAll(args);
    }

    /**
     * 支持多配置文件
     */
    void setConfig(String xml) throws Exception {
        XML parse = XMLParse.parse(xml, argument);
        xmlConfig.addChild(parse);
        xmlIndex = xmlConfig.index(true);
    }

    // getter

    /**
     * @return 返回入口参数值
     */
    public String getArg(String key) {
        return argument.get(key);
    }

    /**
     * @return 返回所有入口参数值对
     */
    public Map<String, String> getArgs() {
        return argument;
    }

    /**
     * @return 返回 XML 对象的 text 值
     */
    public String getValue(String key) {
        List<XML> xml = getTags(key);
        if (xml == null) return null;
        if (xml.size() > 1) {
            logger.warn("multiple tag : " + key);
        }
        return xml.get(0).getText();
    }

    /**
     * @return 返回 List XML
     */
    public List<XML> getTags(String key) {
        return xmlIndex.get(key);
    }

    /**
     * @return 返回 XML 对象的根节点
     */
    public XML getRoot() {
        return xmlIndex.get("__root").get(0);
    }

}
