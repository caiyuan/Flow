package me.caiyuan.flow.repeater;

import me.caiyuan.flow.Flow;
import me.caiyuan.flow.FlowParameter;
import me.caiyuan.flow.pool.FlowBlockingDeque;

/**
 * @author Ryan
 * <p/>
 * 数据转发器
 */
public class FlowTransmit extends Flow {

    public FlowTransmit(String id, FlowParameter param) {
        super(id, param, FlowBlockingDeque.class);
    }

    @Override
    public void process(Object o) {
        handle.push(o);
    }

}
