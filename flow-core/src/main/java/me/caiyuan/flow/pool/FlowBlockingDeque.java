package me.caiyuan.flow.pool;

import me.caiyuan.flow.FlowPool;
import org.apache.log4j.Logger;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Ryan
 */
public class FlowBlockingDeque<T> implements FlowPool<T> {

    private final Logger log = Logger.getLogger(FlowBlockingDeque.class);

    private final AtomicLong total = new AtomicLong();
    private final BlockingDeque<T> resultSet;

    public FlowBlockingDeque() {
        this.resultSet = new LinkedBlockingDeque<>(5120);
    }

    public FlowBlockingDeque(int size) {
        this.resultSet = new LinkedBlockingDeque<>(size);
    }

    @Override
    public void put(T data) {
        try {
            resultSet.putFirst(data);
            total.getAndIncrement();
        } catch (InterruptedException e) {
            log.warn("", e);
        }
    }

    @Override
    public T poll() {
        return resultSet.pollLast();
    }

    @Override
    public long size() {
        return resultSet.size();
    }

    @Override
    public long total() {
        return total.get();
    }

}
