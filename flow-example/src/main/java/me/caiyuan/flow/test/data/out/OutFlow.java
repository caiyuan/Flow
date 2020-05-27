package me.caiyuan.flow.test.data.out;

import me.caiyuan.flow.Flow;
import me.caiyuan.flow.FlowParameter;
import me.caiyuan.flow.test.bean.DataSource;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author Ryan
 */
public class OutFlow extends Flow {

    private Logger log = Logger.getLogger(this.getClass());
    private DataSource dataSource;

    public OutFlow(String id, FlowParameter parameter) {
        super(id, parameter);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void init() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            log.info(id + " " + dataSource.getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void process(Object o) throws Exception {
        Thread.sleep(1000);

        String[] data = (String[]) o;
        log.info(id + " " + Arrays.toString(data) + " " + System.currentTimeMillis());
    }

    @Override
    public void finish() {
        System.out.println(id + " --> finish " + System.currentTimeMillis());
    }

}
