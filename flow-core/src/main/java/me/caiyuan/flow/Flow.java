package me.caiyuan.flow;

import me.caiyuan.flow.pool.FlowInvalid;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ryan
 */
public abstract class Flow extends FlowInit {

    /**
     * 数据分发助手
     */
    protected final FlowHandle handle;
    /**
     * 组件处理器线程组, 在 process 方法中用作顶层线程组
     */
    protected final ThreadGroup procThreads;
    final ReentrantLock INIT_LOCK = new ReentrantLock();
    /**
     * 从上游到该组件的 DataPush
     */
    final ConcurrentHashMap<String, FlowPush> pushLeft;
    volatile Init init = Init.NEW;        // 初始化状态
    volatile Process state = Process.NEW; // 处理器状态

    /**
     * @param id    Flow 唯一标示
     * @param param Flow 参数信息
     */
    public Flow(String id, FlowParameter param) {
        super(id, param);
        this.handle = new FlowHandle(FlowInvalid.class);
        this.procThreads = new ThreadGroup(FlowMonitor.APP_THREADS, id);
        this.pushLeft = new ConcurrentHashMap<>();
    }

    /**
     * @param id    Flow 唯一标示
     * @param param Flow 参数信息
     * @param clz   数据对象推送池
     */
    public Flow(String id, FlowParameter param, Class<? extends FlowPool> clz) {
        super(id, param);
        this.handle = new FlowHandle(clz);
        this.procThreads = new ThreadGroup(FlowMonitor.APP_THREADS, id);
        this.pushLeft = new ConcurrentHashMap<>();
    }


    /**
     * @param id      Flow 唯一标示
     * @param param   Flow 参数信息
     * @param clz     数据对象推送池
     * @param timeout 数据对象推送池超时时间
     */
    public Flow(String id, FlowParameter param, Class<? extends FlowPool> clz, long timeout) {
        super(id, param);
        this.handle = new FlowHandle(clz, timeout);
        this.procThreads = new ThreadGroup(FlowMonitor.APP_THREADS, id);
        this.pushLeft = new ConcurrentHashMap<>();
    }

    /**
     * @param id      Flow 唯一标示
     * @param param   Flow 参数信息
     * @param clz     数据对象推送池
     * @param harmony 多组件是否自动负载均衡
     */
    public Flow(String id, FlowParameter param, Class<? extends FlowPool> clz, boolean harmony) {
        super(id, param);
        this.handle = new FlowHandle(clz, harmony);
        this.procThreads = new ThreadGroup(FlowMonitor.APP_THREADS, id);
        this.pushLeft = new ConcurrentHashMap<>();
    }

    /**
     * @param id      Flow 唯一标示
     * @param param   Flow 参数信息
     * @param clz     数据对象推送池
     * @param harmony 多组件是否自动负载均衡
     * @param timeout 数据对象推送池超时时间
     */
    public Flow(String id, FlowParameter param, Class<? extends FlowPool> clz, boolean harmony, long timeout) {
        super(id, param);
        this.handle = new FlowHandle(clz, harmony, timeout);
        this.procThreads = new ThreadGroup(FlowMonitor.APP_THREADS, id);
        this.pushLeft = new ConcurrentHashMap<>();
    }

    final void register(Flow plugin) throws Exception {
        handle.plugin(this, plugin);
    }

    /**
     * 用于组件内部的初始化
     */
    @Override
    public void init() {
    }

    /**
     * 处理上游组件推送的数据
     */
    public void process(Object o) throws Exception {
    }

    /**
     * 本方法全部上游组件及本组件执行完毕时有框架调度执行
     */
    public void finish() {
    }

    enum Init {
        NEW,        // 完成构建
        LAZY,       // 待初始化
        UNDERWAY,   // 初始化中
        FINISH      // 初始完成
    }

    enum Process {
        NEW,        // 完成构建
        START,      // 入口组件
        RUNNABLE,   // 正在执行
        WAITING,    // 等待执行
        TERMINATED  // 执行结束
    }

}
