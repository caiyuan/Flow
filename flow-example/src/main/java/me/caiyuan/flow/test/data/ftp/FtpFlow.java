package me.caiyuan.flow.test.data.ftp;

import me.caiyuan.flow.Flow;
import me.caiyuan.flow.FlowParameter;
import me.caiyuan.flow.pool.FlowBlockingDeque;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Ryan
 */
public class FtpFlow extends Flow {

    public FtpFlow(String id, FlowParameter parameter) {
        super(id, parameter, FlowBlockingDeque.class);
    }

    @Override
    public void init() {
    }

    @Override
    public void process(Object o) {

        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> fileList = new ArrayList<String>();
                fileList.add("/Users/Ryan/Documents/workspace/Flow/flow-example/vendor/data.txt");
                fileList.add("/Users/Ryan/Documents/workspace/Flow/flow-example/vendor/data.txt");
                fileList.add("/Users/Ryan/Documents/workspace/Flow/flow-example/vendor/data.txt");

                for (final String file : fileList) {
                    // handle.push(file, "fileFlow1");
                    handle.push(file);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, id + "#test").start();

    }

    @Override
    public void finish() throws Exception {
        System.out.println(id + " --> finish");
    }

}
