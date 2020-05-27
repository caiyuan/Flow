package me.caiyuan.flow;

/**
 * @author Ryan
 */
public abstract class FlowConstructor {

    protected final String id;
    protected final FlowParameter param;

    protected FlowConstructor(String id, FlowParameter param) {
        this.id = id;
        this.param = param;
    }

}
