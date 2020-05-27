package me.caiyuan.flow.test.data.file;

import me.caiyuan.flow.Flow;
import me.caiyuan.flow.FlowParameter;
import me.caiyuan.flow.test.pool.ObjectPool;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Ryan
 */
public class FileFlow extends Flow {

    private final FileConfig fileConfig;

    public FileFlow(String id, FlowParameter parameter) {
        super(id, parameter, ObjectPool.class);
        fileConfig = FileConfig.parse(parameter);
    }

    @Override
    public void init() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "file!init!thread").start();

        // Thread.sleep(1000);
        System.out.println(id + " init.");
    }

    @Override
    public void process(Object o) {

        String filePath = (String) o;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                final String[] data = line.split(fileConfig.separator);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handle.push(data, true);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }, id + "#test" + ++count).start();

//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void finish() throws Exception {
        System.out.println(id + " --> finish");
    }

}
