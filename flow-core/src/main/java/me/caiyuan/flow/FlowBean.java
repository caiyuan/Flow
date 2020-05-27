package me.caiyuan.flow;

/**
 * @author Ryan
 */
public abstract class FlowBean extends FlowInit {

    public FlowBean(String id, FlowParameter param) {
        super(id, param);
    }

    @Override
    public void init() throws Exception {
    }

}
