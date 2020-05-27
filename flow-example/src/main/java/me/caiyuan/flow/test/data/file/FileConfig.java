package me.caiyuan.flow.test.data.file;

import me.caiyuan.flow.FlowParameter;

/**
 * @author Ryan
 */
public class FileConfig {

    public final String args;
    public final String separator;

    private FileConfig(String args, String separator) {
        this.args = args;
        this.separator = separator;
    }

    static FileConfig parse(FlowParameter parameter) {
        // args
        String args = parameter.getArg("args");
        // xml
        String separator = parameter.getValue("sep");

        return new FileConfig(args, separator);
    }

}
