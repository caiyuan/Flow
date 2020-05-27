package me.caiyuan.flow.pool;

import me.caiyuan.flow.FlowPool;

/**
 * @author Ryan
 */
public class FlowInvalid<T> implements FlowPool<T> {

    @Override
    public void put(T data) {
    }

    @Override
    public T poll() {
        return null;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public long total() {
        return 0;
    }

}
