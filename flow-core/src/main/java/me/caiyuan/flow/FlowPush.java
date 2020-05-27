package me.caiyuan.flow;

import org.apache.log4j.Logger;

import java.util.concurrent.locks.ReentrantLock;

import static me.caiyuan.flow.Flow.Init.FINISH;
import static me.caiyuan.flow.Flow.Init.UNDERWAY;
import static me.caiyuan.flow.Flow.Process;

/**
 * @author Ryan
 */
public class FlowPush extends Thread {

    private final Logger logger = Logger.getLogger(FlowPush.class);

    private final Flow tf;
    private final Flow plugin;
    private final FlowPool pool;
    private final long timeout;

    public FlowPush(Flow tf, Flow plugin, FlowPool pool, long timeout) {
        super(plugin.procThreads, plugin.id + " <== " + tf.id);
        this.tf = tf;
        this.plugin = plugin;
        this.pool = pool;
        this.timeout = timeout;
        this.plugin.pushLeft.put(tf.id, this);
    }

    public void put(Object data) {
        pool.put(data);
    }

    public synchronized void keepStart() {
        State state = getState();
        if (state == State.TIMED_WAITING) {
            notifyAll();
        } else if (state == State.NEW) {
            logger.debug("启动数据推 " + getName());
            start();
        } else if (state == State.TERMINATED) {
            throw new IllegalThreadStateException("数据推已退出 " + getName());
        }
    }

    @Override
    public void run() {
        logger.debug("进入数据推 " + getName());
        try {
            byte detection = -1;
            while (true) {
                Object data;
                while ((data = pool.poll()) != null) {
                    detection = 0;
                    if (plugin.init == FINISH) {
                        plugin.state = Process.RUNNABLE;
                        plugin.process(data);
                    } else {
                        ReentrantLock lock = plugin.INIT_LOCK;
                        lock.lock();
                        try {
                            if (plugin.init != FINISH) {
                                logger.info("init : " + plugin.id);
                                plugin.init = UNDERWAY;
                                plugin.init();
                                plugin.init = FINISH;
                            }
                        } finally {
                            lock.unlock();
                        }
                        plugin.state = Process.RUNNABLE;
                        plugin.process(data);
                    }
                }
                synchronized (this) {
                    if (detection == 1 &&
                            (tf.state == Process.TERMINATED || tf.state == Process.START))
                        break;
                    plugin.state = Process.WAITING;
                    wait(timeout);
                    detection = 1;
                }
            }

            logger.debug("数据推退出 " + getName());

        } catch (InterruptedException e) {
            logger.error("数据推异常退出,重新进入 " + getName(), e);
            run();
        } catch (Exception e) {
            logger.error("数据推异常退出 " + getName(), e);
            System.exit(1);
        } finally {
            plugin.state = Process.TERMINATED;
        }
    }


    public FlowPush duplicate() {
        return new FlowPush(tf, plugin, pool, timeout);
    }

}
