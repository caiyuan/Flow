package me.caiyuan.flow;

import org.apache.log4j.Logger;

import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Ryan
 */
public class FlowMonitor {

    final static ThreadGroup APP_THREADS = new ThreadGroup("APP_THREADS");
    final static Map<String, FlowState> flowState = new HashMap<String, FlowState>();
    final static Map<String, List<Object[]>> monitorPool = new LinkedHashMap<String, List<Object[]>>();
    private static final Logger log = Logger.getLogger(FlowMonitor.class);
    /**
     * 是否为显示数据池数据量信息
     */
    static boolean develop = false;

    /**
     * 注册需要监控的数据池
     *
     * @param id     上有组件标识
     * @param plugin 下游组件标识
     * @param pool   关联的数据池
     */
    static void monitor(String id, String plugin, FlowPool pool) {
        List<Object[]> pools = monitorPool.get(plugin);
        if (pools == null) {
            pools = new ArrayList<Object[]>();
            monitorPool.put(plugin, pools);
        }
        pools.add(new Object[]{id, pool});
    }

    /**
     * 获取依赖的所有上游组件(包含上游的上游组件)
     */
    private static Set<String> depend(FlowConfig tc, List<FlowConfig> flowConfigs) {
        Set<String> depend = new HashSet<String>();
        String id = tc.getId();
        for (FlowConfig config : flowConfigs) {
            Set<String> pluginList = config.getPluginList();
            for (String plugin : pluginList) {
                if (id.equals(plugin)) {
                    depend.add(config.getId());
                    depend.addAll(depend(config, flowConfigs));
                    break;
                }
            }
        }
        return depend;
    }

    /**
     * 注册需要监控的组件
     */
    static void monitor(List<FlowConfig> flowConfigs, Map<String, Flow> flowList) {
        for (FlowConfig config : flowConfigs) {
            String id = config.getId();
            Flow flow = flowList.get(id);
            ThreadGroup procThreads = flow.procThreads;
            Set<String> dependFlowAll = depend(config, flowConfigs);
            flowState.put(id, new FlowState(id, flow, procThreads, dependFlowAll.toArray(new String[dependFlowAll.size()])));
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int initial = -1;
                    while (true) {
                        int activeCount = APP_THREADS.activeCount();

                        // state
                        for (String id : flowState.keySet()) {
                            FlowState flowState = FlowMonitor.flowState.get(id);
                            ThreadGroup procThreads = flowState.procThreads;
                            int actionCount = procThreads.activeCount();
                            flowState.active = actionCount != 0;
                        }

                        // init
                        if (develop && initial != 0) {
                            initial = 0;
                            StringBuilder monitor = new StringBuilder("AppMonitor: init");
                            for (String id : flowState.keySet()) {
                                FlowState flowState = FlowMonitor.flowState.get(id);
                                Flow.Init init = flowState.flow.init;
                                if (init != Flow.Init.FINISH) initial = initial + 1;
                                else continue;
                                monitor.append("\n\t");
                                monitor.append(flowState.id);
                                monitor.append(" ");
                                monitor.append(init);
                            }
                            if (initial > 0) log.info(monitor);
                        }

                        // pool
                        if (develop) {
                            StringBuilder monitor = new StringBuilder("AppMonitor: pool");
                            for (String id : monitorPool.keySet()) {
                                FlowState flowState = FlowMonitor.flowState.get(id);
                                monitor.append("\n\t");
                                switch (flowState.flow.state) {
                                    case NEW:
                                        monitor.append("N");
                                        break;
                                    case START:
                                        monitor.append("S");
                                        break;
                                    case RUNNABLE:
                                        monitor.append("R");
                                        break;
                                    case WAITING:
                                        monitor.append("W");
                                        break;
                                    case TERMINATED:
                                        monitor.append("T");
                                        break;
                                }
                                monitor.append("'");
                                monitor.append(id);
                                monitor.append(" ");
                                List<Object[]> depends = monitorPool.get(id);
                                for (Object[] item : depends) {
                                    String depend = (String) item[0];
                                    FlowState dependFlowState = FlowMonitor.flowState.get(depend);
                                    FlowPush dependFlowPush = flowState.flow.pushLeft.get(depend);
                                    FlowPool pool = (FlowPool) item[1];
                                    // 上游PROC的状态
                                    if (dependFlowState.active) {
                                        monitor.append("A"); // Active
                                    } else {
                                        monitor.append("D"); // Disable
                                    }
                                    monitor.append(",");
                                    // 上游数据退状态
                                    switch (dependFlowPush.getState()) {
                                        case NEW:
                                            monitor.append("N");
                                            break;
                                        case RUNNABLE:
                                            monitor.append("R");
                                            break;
                                        case BLOCKED:
                                            monitor.append("B");
                                            break;
                                        case WAITING:
                                            monitor.append("W");
                                            break;
                                        case TIMED_WAITING:
                                            monitor.append("W");
                                            break;
                                        case TERMINATED:
                                            monitor.append("T");
                                            break;
                                    }
                                    monitor.append("'");
                                    // 上游数据池容量
                                    monitor.append(depend);
                                    monitor.append("[");
                                    monitor.append(pool.size());
                                    monitor.append(",");
                                    monitor.append(pool.total());
                                    monitor.append("]");
                                    monitor.append(" ");
                                }
                            }
                            log.info(monitor);
                        }

                        // finish
                        for (String id : flowState.keySet()) {
                            FlowState flowState = FlowMonitor.flowState.get(id);
                            if (!flowState.active && flowState.finish) {
                                boolean flag = false;
                                for (String depend : flowState.dependFlowAll) {
                                    if (FlowMonitor.flowState.get(depend).active) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if (flag) continue;

                                flowState.finish = false;
                                final Flow tf = flowState.flow;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            tf.finish();
                                        } catch (Exception e) {
                                            log.error("AppMonitor", e);
                                        }
                                    }
                                }, tf.id + "#finish").start();
                            }
                        }

                        if (activeCount == 0) break;

                        SECONDS.sleep(5);
                    }
                } catch (Exception e) {
                    new Thread(this, "AppMonitor").start();
                    log.error("AppMonitor", e);
                }
            }
        }, "AppMonitor").start();
    }

}

/**
 * 组件的线程状态信息
 */
class FlowState {
    /**
     * 组件的标示
     */
    final String id;
    /**
     * 组件的引用
     */
    final Flow flow;
    /**
     * 顶级线程组
     */
    final ThreadGroup procThreads;
    /**
     * 依赖的组件(包含上游的上游)
     */
    final String[] dependFlowAll;
    /**
     * 是否需清理
     */
    volatile boolean finish;
    /**
     * 有活动线程
     */
    volatile boolean active;

    public FlowState(String id, Flow flow, ThreadGroup procThreads, String[] dependFlowAll) {
        this.id = id;
        this.flow = flow;
        this.procThreads = procThreads;
        this.dependFlowAll = dependFlowAll;
        this.finish = true;
        this.active = false;
    }

}
