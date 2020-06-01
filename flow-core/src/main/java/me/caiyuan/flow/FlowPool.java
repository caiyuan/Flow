package me.caiyuan.flow;

/**
 * @author Ryan
 */
public interface FlowPool<T> {

    /**
     * 向数据池推放数据
     */
    void put(T data);

    /**
     * 数据池为空则返回NULL值
     */
    T poll();

    /**
     * 当前数据池的记录数
     */
    long size();

    /**
     * 向数据池推放的总记录数
     */
    long total();

}
