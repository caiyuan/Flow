package me.caiyuan.flow;

import me.caiyuan.flow.util.FlowUtil;
import me.caiyuan.flow.xml.XMLParse;
import org.apache.log4j.Logger;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ryan
 */
public class FlowApp {

    private static Logger logger = Logger.getLogger(FlowApp.class);

    public static void main(final String[] args) throws Exception {

        logger.info("& 载入配置数据 ..");
        Map<String, String> arg = FlowUtil.map(args);

        String applicationConfigPath = arg.get("applicationConfigPath");
        if (applicationConfigPath == null) applicationConfigPath = "./applicationConfig.xml";

        String start = XMLParse.parse(applicationConfigPath).index().get("applicationConfig").get(0).getAttribute("start");
        if (start == null || start.trim().equals("")) throw new Exception("未指定系统的入口组件");

        Map<FlowConfig.Type, List<FlowConfig>> applicationConfig = FlowConfig.parse(applicationConfigPath);

        logger.info("& 初始化 Argument 组件 ..");
        List<FlowConfig> valueConfigs = applicationConfig.get(FlowConfig.Type.Argument);
        Map<String, FlowArgument> argumentList = getInstance(valueConfigs, arg);
        Map<String, String> argument = argument(argumentList);


        Map<String, Object> module;

        logger.info("& 初始化 Bean 组件 ..");
        List<FlowConfig> beanConfigs = applicationConfig.get(FlowConfig.Type.Bean);
        Map<String, FlowBean> beanList = getInstance(beanConfigs, argument);
        module = new HashMap<String, Object>();
        module.putAll(beanList);
        inject(beanConfigs, module);
        init(beanConfigs, beanList);

        logger.info("& 初始化 Flow 组件 ..");
        List<FlowConfig> tfConfigs = applicationConfig.get(FlowConfig.Type.Flow);
        Map<String, Flow> flowList = getInstance(tfConfigs, argument);
        module = new HashMap<String, Object>();
        module.putAll(beanList);
        module.putAll(flowList);
        inject(tfConfigs, module);
        init(tfConfigs, flowList);
        register(tfConfigs, flowList);

        logger.info("& 启动程序 ..");
        start(start, flowList);


        String develop = argument.get("develop");
        FlowMonitor.develop = develop != null && develop.equals("true");

        FlowMonitor.monitor(tfConfigs, flowList);
    }

    private static Map<String, String> argument(Map<String, FlowArgument> argumentList) throws Exception {
        Map<String, String> argument = new HashMap<String, String>();
        for (FlowArgument flowArgument : argumentList.values()) {
            Map<String, String> values = flowArgument.process();
            if (values != null) argument.putAll(values);
        }
        return argument;
    }

    private static <T extends FlowConstructor> Map<String, T> getInstance(List<FlowConfig> flowConfigs, Map<String, String> argument) throws Exception {
        Map<String, T> result = new TreeMap<String, T>();
        for (FlowConfig flowConfig : flowConfigs) {
            logger.info("create : " + flowConfig.getId() + " # " + flowConfig.getClz());
            FlowParameter param = new FlowParameter(argument);
            if (flowConfig.getConfigList() != null) {
                for (String config : flowConfig.getConfigList()) {
                    param.setConfig(config);
                }
            }

            Class<T> clz = (Class<T>) Class.forName(flowConfig.getClz());
            Constructor<T> flowConstructor = clz.getConstructor(String.class, FlowParameter.class);
            T module = flowConstructor.newInstance(flowConfig.getId(), param);

            result.put(flowConfig.getId(), module);
        }
        return result;
    }

    private static void inject(List<FlowConfig> flowConfigs, Map<String, ?> flowList) throws Exception {
        for (FlowConfig config : flowConfigs) {
            Map<String, String> beanMap = config.getBeanMap();
            Object flow = flowList.get(config.getId());
            BeanInfo beanInfo = Introspector.getBeanInfo(flow.getClass());
            PropertyDescriptor[] des = beanInfo.getPropertyDescriptors();
            for (String name : beanMap.keySet()) {
                String value = beanMap.get(name);
                for (PropertyDescriptor de : des) {
                    if (name.equals(de.getName())) {
                        logger.info("inject : " + config.getId() + " <-- " + value);
                        Method method = de.getWriteMethod();
                        Object bean = flowList.get(value);
                        if (bean == null) throw new Exception("未知的 Bean 组件 : " + value);
                        method.invoke(flow, bean);
                    }
                }
            }
        }
    }

    private static void init(List<FlowConfig> flowConfigs, Map<String, ? extends FlowInit> flowList) throws Exception {
        List<FlowConfig> beans = new ArrayList<FlowConfig>();
        List<FlowConfig> tfs = new ArrayList<FlowConfig>();
        for (FlowConfig config : flowConfigs) {
            if (FlowConfig.Type.Bean.equals(config.getType())) {
                beans.add(config);
            }
            if (FlowConfig.Type.Flow.equals(config.getType())) {
                tfs.add(config);
            }
        }
        for (FlowConfig config : beans) {
            logger.info("init : " + config.getId());
            flowList.get(config.getId()).init();
        }
        for (final FlowConfig config : tfs) {
            final Flow flow = (Flow) flowList.get(config.getId());
            final ReentrantLock lock = flow.INIT_LOCK;
            lock.lock();
            try {
                if (config.isLazy()) {
                    logger.info("lazy : " + config.getId());
                    flow.init = Flow.Init.LAZY;
                    continue;
                }

                new Thread(new Runnable() {
                    public void run() {
                        lock.lock();
                        try {
                            if (flow.init != Flow.Init.LAZY && flow.init != Flow.Init.FINISH) {
                                logger.info("init : " + flow.id);
                                flow.init = Flow.Init.UNDERWAY;
                                flow.init();
                                flow.init = Flow.Init.FINISH;
                            }
                        } catch (Exception e) {
                            logger.error("初始化失败: " + flow.id, e);
                            System.exit(1);
                        } finally {
                            lock.unlock();
                        }
                    }
                }, flow.id + "#init").start();

            } finally {
                lock.unlock();
            }
        }
    }

    private static void register(List<FlowConfig> flowConfigs, Map<String, Flow> flowList) throws Exception {
        for (FlowConfig config : flowConfigs) {
            if (config.getPluginList() != null) {
                Flow flow = flowList.get(config.getId());
                for (String pid : config.getPluginList()) {
                    logger.info("register : " + config.getId() + " <-- " + pid);
                    Flow plugin = flowList.get(pid);
                    if (pid == null) throw new Exception("未知的 Flow 组件 : " + pid);
                    flow.register(plugin);
                }
            }
        }
    }

    private static void start(String start, Map<String, Flow> flowList) throws Exception {
        final Flow main = flowList.get(start);
        if (main == null) throw new Exception("未知的 Flow 组件 : " + start);
        new Thread(main.procThreads, new Runnable() {
            @Override
            public void run() {
                try {
                    main.state = Flow.Process.START;
                    main.process("null");
                } catch (Exception e) {
                    logger.error("App Error : ", e);
                    System.exit(1);
                }
            }
        }, main.id + " <== appStart").start();
    }

}
