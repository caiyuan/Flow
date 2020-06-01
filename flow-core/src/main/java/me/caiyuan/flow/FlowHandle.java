package me.caiyuan.flow;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ryan
 */
public class FlowHandle {

    private final ReentrantLock lock = new ReentrantLock();
    private final List<FlowMap> pushList;
    private final Class<? extends FlowPool> clz;
    private final Logger log = Logger.getLogger(FlowHandle.class);
    private FlowPool pool;
    private boolean harmony = false;
    private long timeout = 5 * 1000;
    /**
     * 下一个接受 DataPush送的组件的位置
     */
    private int flag = 0;

    FlowHandle(Class<? extends FlowPool> clz) {
        this.clz = clz;
        this.pushList = new CopyOnWriteArrayList<>();
    }

    FlowHandle(Class<? extends FlowPool> clz, boolean harmony) {
        this.clz = clz;
        this.harmony = harmony;
        this.pushList = new CopyOnWriteArrayList<>();
    }

    public FlowHandle(Class<? extends FlowPool> clz, long timeout) {
        this.clz = clz;
        if (timeout > 0) this.timeout = timeout;
        this.pushList = new CopyOnWriteArrayList<>();
    }


    FlowHandle(Class<? extends FlowPool> clz, boolean harmony, long timeout) {
        this.clz = clz;
        this.harmony = harmony;
        if (timeout > 0) this.timeout = timeout;
        this.pushList = new CopyOnWriteArrayList<>();
    }

    void plugin(Flow tf, Flow plugin) throws Exception {
        FlowPool _pool = harmony ? (pool != null ? pool : (pool = clz.newInstance())) : clz.newInstance();
        FlowPush push = new FlowPush(tf, plugin, _pool, timeout);
        pushList.add(new FlowMap(plugin, push));
        FlowMonitor.monitor(tf.id, plugin.id, _pool);
    }

    private synchronized void push(Object data, FlowMap flowMap) {
        FlowMap active;
        try {
            flowMap.push.keepStart();
            active = flowMap;
        } catch (IllegalThreadStateException e) {
            log.debug("重启 DataPush " + flowMap.push.getName());
            FlowMap duplicate = flowMap.duplicate();
            duplicate.push.keepStart();
            int index = pushList.indexOf(flowMap);
            pushList.set(index, duplicate);
            active = duplicate;
        }
        active.push.put(data);
    }

    /**
     * 向组件推送同一份数据
     */
    public void push(Object data) {
        if (data == null) return;
        lock.lock();
        try {
            for (FlowMap flowMap : pushList) {
                push(data, flowMap);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 向指定组件推送数据
     */
    public void push(Object data, String id) {
        if (data == null) return;
        lock.lock();
        try {
            for (FlowMap flowMap : pushList) {
                if (id.equals(flowMap.plugin.id)) {
                    push(data, flowMap);
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param random : true, 顺序向组件推送数据
     * @see FlowHandle#push(Object)
     */
    public void push(Object data, boolean random) {
        if (data == null) return;
        lock.lock();
        try {
            int size = pushList.size();
            if (size == 1 || !random) {
                this.push(data);
                return;
            }
            if (flag == 0) flag = size;
            int index = 1;
            for (FlowMap flowMap : pushList) {
                if (index == flag) {
                    push(data, flowMap);
                    flag = flag - 1;
                    return;
                }
                index = index + 1;
            }
        } finally {
            lock.unlock();
        }
    }

    private static class FlowMap {
        private final Flow plugin;
        private final FlowPush push;
        private boolean initial = false;

        private FlowMap(Flow plugin, FlowPush push) {
            this.plugin = plugin;
            this.push = push;
        }

        private FlowMap duplicate() {
            FlowMap duplicate = new FlowMap(plugin, push.duplicate());
            duplicate.initial = initial;
            return duplicate;
        }
    }

}
