package com.xxx.framework.spring.content;

import com.xxx.framework.annotation.TAutowired;
import com.xxx.framework.aop.TJDKDynamicAopProxy;
import com.xxx.framework.aop.config.TAopConfig;
import com.xxx.framework.aop.support.TAdvisedSupport;
import com.xxx.framework.beans.TBeanWarpper;
import com.xxx.framework.beans.config.TBeanDefinition;
import com.xxx.framework.beans.support.TBeanDefinitionReader;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * new TestApplicationContext("classpath:application.properties") 方式调用
 */
public class TestApplicationContext {

    //保存application.properties配置文件中的内容
    public TBeanDefinitionReader reader;
    //注册信息保存
    Map<String, TBeanDefinition> beanDefinitionMap = new HashMap<String, TBeanDefinition>();
    //传说中的IOC容器 beanName -> TBeanWarpper 设计
    public Map<String, TBeanWarpper> iocCacheMap = new HashMap<String, TBeanWarpper>();
    //beanName -> 实例化的类对象
    public Map<String, Object> iocObjectCacheMap = new HashMap<String, Object>();
    //被代理对象的原始对象 最后需要反射注入
    public List<TBeanWarpper> targetCacheList = new ArrayList<TBeanWarpper>();

    public TestApplicationContext(String... classpaths) {
        init(classpaths);
    }

    public void init(String... classpaths) {
        try {
            //1.读取配置文件 加载配置 && 扫描对应的类
            this.reader = new TBeanDefinitionReader(classpaths);
            //2.解析类路径下的class对象(不做实例化) 把beanName和className保存起来
            List<TBeanDefinition> beanDefinitions = this.reader.loadBeanDefinitions();
            //3.存储到注册信息map ioc到这里就完成了 接下来就是依赖注入阶段了
            doRegistBeanDefinition(beanDefinitions);
            //4.依赖注入
            doAutowired();

            System.out.println("application is init");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAutowired() {
        //配置阶段完成 真正开始实例化对象
        for (Map.Entry<String, TBeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            getBean(beanName);
        }
        //被代理的原始类 如果有声明 也需要注入
        for (TBeanWarpper warpper : this.targetCacheList) {
            populateBean(null, null, warpper);
        }
    }

    //开始Bean的实例化 和 DI依赖注入
    public Object getBean(String beanName) {
        //1.从缓存中拿到TBeanDefinition信息
        TBeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);
        //2.实例化对象
        Object instance = instanceBean(beanDefinition);

        //3.封装成TBeanWarpper对象 存储class对象 和 实例化对象
        TBeanWarpper beanWarpper = new TBeanWarpper(instance);

        //4.保存到IOC容器
        iocCacheMap.put(beanName, beanWarpper);

        //5.执行依赖注入
        populateBean(beanName, beanDefinition, beanWarpper);

        return instance;
    }

    //实例化 && AOP在此完成
    private Object instanceBean(TBeanDefinition beanDefinition) {
        String beanName = beanDefinition.getFactoryBeanName();
        if (beanDefinition.getInterfaceImplBeanName() != null) {   //接口取实现类的beanName 保持单列
            beanName = beanDefinition.getInterfaceImplBeanName();
        }
        String beanClassName = beanDefinition.getBeanClassName();
        Object instance = null;
        try {
            //先从实例化对象容器中取
            if (this.iocObjectCacheMap.containsKey(beanName)) {
                instance = this.iocObjectCacheMap.get(beanName);
            } else {
                Class<?> clazz = Class.forName(beanClassName);
                //如果满足AOP切面条件 此处应该生成代理对象,那么最后调用处invoke应该自己实现,里面需要保存原有的实例对象
                instance = checkAopCase(clazz);

                //beanName和beanClassName都存一份 保持单例
                this.iocObjectCacheMap.put(beanName, instance);
                this.iocObjectCacheMap.put(beanClassName, instance);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    private Object checkAopCase(Class<?> clazz) throws Exception {
        Object instance = clazz.newInstance();
        //获取切面切点 正则匹配全类名 匹配上就生成代理对象
        Properties config = this.reader.getConfig();
        String pointCut = config.getProperty("pointCut");

        String regxMethodStr = pointCut.replaceAll("\\.", "\\\\.")
                .replaceAll("\\\\.\\*", ".*")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)");

        //类名是 class com.xxx.demo.service.impl.HelloServiceImp 所以匹配类名做如下处理
        String regxClassStr = regxMethodStr.substring(0, regxMethodStr.lastIndexOf("\\("));
        Pattern classPattern = Pattern.compile("class " + regxClassStr.substring(regxClassStr.lastIndexOf(" ") + 1));
        if (classPattern.matcher(instance.getClass().toString()).matches()) {
            TAopConfig aopConfig = new TAopConfig();
            aopConfig.setPointCut(pointCut);
            aopConfig.setAspectClass(config.getProperty("aspectClass"));
            aopConfig.setAspectBefore(config.getProperty("aspectBefore"));
            aopConfig.setAspectAfter(config.getProperty("aspectAfter"));
            aopConfig.setAspectAfterThrowing(config.getProperty("aspectAfterThrowing"));

            TAdvisedSupport advisedSupport = new TAdvisedSupport(aopConfig);
            //匹配方法的正则
            Pattern methodPattern = Pattern.compile(regxMethodStr);
            advisedSupport.setMethodPattern(methodPattern);
            advisedSupport.setTarget(instance);
            advisedSupport.setTargetClass(clazz);

            //被代理的保留原始实例化对象 如果有声明其他类 需要注入
            Object target = instance;
            this.targetCacheList.add(new TBeanWarpper(target));
            //匹配上的生成代理对象
            instance = new TJDKDynamicAopProxy(advisedSupport).getProxy();
        }
        return instance;
    }


    private void populateBean(String beanName, TBeanDefinition beanDefinition, TBeanWarpper beanWarpper) {
        Class<?> clazz = beanWarpper.getWarpperClass();
        Object instance = beanWarpper.getWarpperInstance();

        //1.前面加了注解的的类已经实例化完成 这里判断需要注入的声明即可
        //获取类里面所有的声明对象 如果有TAutowired注解 反射赋值
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(TAutowired.class)) continue;

            //TAutowired这里加了个value 可以自定义beanName 等同于Qualifier("xxx")
            TAutowired autowired = field.getAnnotation(TAutowired.class);
            String autowireNname = field.getType().getName();
            if (!"".equals(autowired.value())) {
                autowireNname = autowired.value();
            }

            //延时加载 或者循环依赖 导致IOC容器还没有实例化的 在下次调用才会初始化
            if (!this.iocCacheMap.containsKey(autowireNname)) continue;

            field.setAccessible(true);
            try {
                field.set(instance, this.iocCacheMap.get(autowireNname).getWarpperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void doRegistBeanDefinition(List<TBeanDefinition> beanDefinitions) throws Exception {
        for (TBeanDefinition beanDefinition : beanDefinitions) {
            if (this.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())) {
                throw new Exception("Create bean Exception ! The bean " + beanDefinition.getFactoryBeanName() + " is exists!!!");
            }
            this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
            //类名全路径也保存一份
            this.beanDefinitionMap.put(beanDefinition.getBeanClassName(), beanDefinition);
        }
    }

    public Object getBean(Class<?> clazz) {
        return this.getBean(clazz.getName());
    }

    public Map<String, TBeanDefinition> getBeanDefinitionMap() {
        return this.beanDefinitionMap;
    }

    public Properties getConfig() {
        return this.reader.getConfig();
    }

}
