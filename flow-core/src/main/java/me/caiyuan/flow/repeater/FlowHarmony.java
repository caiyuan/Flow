package me.caiyuan.flow.repeater;

import me.caiyuan.flow.Flow;
import me.caiyuan.flow.FlowParameter;
import me.caiyuan.flow.pool.FlowBlockingDeque;

/**
 * @author Ryan
 * <p/>
 * 数据均衡器
 */
public class FlowHarmony extends Flow {

    public FlowHarmony(String id, FlowParameter param) {
        super(id, param, FlowBlockingDeque.class, true);
    }

    @Override
    public void process(Object o) {
        handle.push(o, true);
    }

}
