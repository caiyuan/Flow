package me.caiyuan.flow.test.bean;

import me.caiyuan.flow.FlowBean;
import me.caiyuan.flow.FlowParameter;
import me.caiyuan.flow.xml.XML;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan
 */
public class DataSource extends FlowBean {

    private DSConfig dsConfigs;

    public DataSource(String id, FlowParameter param) {
        super(id, param);
        this.dsConfigs = DSConfig.parse(param);
    }

    @Override
    public void init() throws Exception {
        Class.forName(dsConfigs.driverClass);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dsConfigs.jdbcUrl);
    }


    // DataSource Config
    private static class DSConfig {

        private final String driverClass;
        private final String jdbcUrl;


        private DSConfig(String driverClass, String jdbcUrl) {
            this.driverClass = driverClass;
            this.jdbcUrl = jdbcUrl;
        }

        private static DSConfig parse(FlowParameter param) {
            List<XML> prop = param.getTags("property");
            Map<String, String> values = new HashMap<String, String>();
            for (XML xml : prop) {
                String name = xml.getAttribute("name");
                String value = xml.getText();
                values.put(name, value);
            }

            String driverClass = values.get("driverClass");
            String jdbcUrl = values.get("jdbcUrl");

            return new DSConfig(driverClass, jdbcUrl);
        }
    }

}
