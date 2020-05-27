package me.caiyuan.flow;

/**
 * @author Ryan
 */
public abstract class FlowInit extends FlowConstructor {

    protected FlowInit(String id, FlowParameter param) {
        super(id, param);
    }

    public abstract void init() throws Exception;

}
