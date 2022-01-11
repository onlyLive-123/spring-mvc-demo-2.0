package com.xxx.framework.beans.support;

import com.xxx.framework.annotation.TController;
import com.xxx.framework.annotation.TService;
import com.xxx.framework.beans.config.TBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TBeanDefinitionReader {


    //保存扫描的所有的类名
    private List<String> beanDefinitionNames = new ArrayList<String>();
    private Properties contextConfig = new Properties();


    public TBeanDefinitionReader(String[] classpaths) {
        //初始化配置
        doLoadConfig(classpaths[0]);
        //扫描对应的类
        doScanner(contextConfig.getProperty("packscanner"));
    }

    private void doScanner(String packscanner) {
        URL url = this.getClass().getResource("/" + packscanner.replaceAll("\\.", "/"));
        File file = new File(url.getPath());
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                //com.xxx.service  -> com.xxx.service.action
                doScanner(packscanner + "." + f.getName());
            } else {
                if (!f.getName().endsWith(".class")) continue;
                this.beanDefinitionNames.add(packscanner + "." + f.getName().replaceAll("\\.class", ""));
            }
        }
    }

    private void doLoadConfig(String packetName) {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(packetName.replace("classPath:", ""));
        try {
            contextConfig.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<TBeanDefinition> loadBeanDefinitions() {
        List<TBeanDefinition> beanDefinitions = new ArrayList<TBeanDefinition>();
        for (String className : this.beanDefinitionNames) {
            try {
                //这里只解析成class 不做实例化
                //存储 beanName 和className
                Class<?> clazz = Class.forName(className);

                //需要注册的类 应该是加了注解的类 接口要排除掉
                //只有加了注解的类才需要实例化 TController TService
                if (!(clazz.isAnnotationPresent(TController.class)
                        || clazz.isAnnotationPresent(TService.class)) || clazz.isInterface())
                    continue;

                String beanName = toFristLowerCase(clazz.getSimpleName());
                if (clazz.isAnnotationPresent(TService.class)) {
                    TService service = clazz.getAnnotation(TService.class);
                    if (!"".equals(service.value())) {
                        beanName = service.value();
                    }
                }

                beanDefinitions.add(new TBeanDefinition(beanName, clazz.getName()));

                //如果有实现接口的 需要加载接口 接口全名当key 同时存入实现类beanName 注入需要用到
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> i : interfaces) {
                    beanDefinitions.add(new TBeanDefinition(i.getName(), clazz.getName(), beanName));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return beanDefinitions;
    }

    //排除类名小写开头
    private String toFristLowerCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return new String(chars);
    }

    public Properties getConfig() {
        return contextConfig;
    }
}
