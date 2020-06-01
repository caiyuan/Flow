package me.caiyuan.flow.repeater;

import me.caiyuan.flow.Flow;
import me.caiyuan.flow.FlowParameter;
import me.caiyuan.flow.pool.FlowBlockingDeque;

/**
 * @author Ryan
 * <p/>
 * 数据分发器
 */
public class FlowDistribute extends Flow {

    public FlowDistribute(String id, FlowParameter param) {
        super(id, param, FlowBlockingDeque.class);
    }

    @Override
    public void process(Object o) {
        handle.push(o, true);
    }

}
