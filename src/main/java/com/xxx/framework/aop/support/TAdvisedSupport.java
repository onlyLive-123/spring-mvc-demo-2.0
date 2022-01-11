package com.xxx.framework.aop.support;

import com.xxx.framework.aop.aspect.TAdvice;
import com.xxx.framework.aop.config.TAopConfig;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * invoke调用时获取切面方法的
 */
@Data
public class TAdvisedSupport {
    TAopConfig aopConfig;
    Object target;      //原始实例对象
    Class<?> targetClass;   //原始类信息
    Pattern methodPattern;  //匹配方法的正则
    Map<Method, Map<String, List<TAdvice>>> methodAdvicesCache = new HashMap<Method, Map<String, List<TAdvice>>>();

    public TAdvisedSupport(TAopConfig aopConfig) {
        this.aopConfig = aopConfig;
    }


    private void parse() {
        //1.解析切面类对象
        String aspectClass = this.aopConfig.getAspectClass();

        try {
            Class<?> clazz = Class.forName(aspectClass);
            Object instance = clazz.newInstance();
            //2.保存切面类方法和class对象
            Map<String, Method> aspectMethodMap = new HashMap<String, Method>();
            for (Method method : clazz.getMethods()) {
                aspectMethodMap.put(method.getName(), method);
            }
            //3.包装原始类的方法
            Map<String, List<TAdvice>> adviceMaps = new HashMap<String, List<TAdvice>>();
            List<TAdvice> advices = null;
            for (Method method : this.targetClass.getMethods()) {
                String methodStr = method.toString();
                if (methodStr.contains("throws")) {   //如果方法名有异常throws的 在这里截断
                    methodStr = methodStr.substring(0, methodStr.lastIndexOf("throws") - 1);
                }
                //通过方法路径匹配方法正则
                if (this.methodPattern.matcher(methodStr).matches()) {
                    adviceMaps = new HashMap<String, List<TAdvice>>();
                    //前置通知
                    String before = this.aopConfig.getAspectBefore();
                    if (aspectMethodMap.containsKey(before)) {
                        advices = adviceMaps.get(before);
                        if(advices == null) advices = new ArrayList<TAdvice>();
                        advices.add(new TAdvice(instance, aspectMethodMap.get(before)));
                        adviceMaps.put(before, advices);
                    }
                    //后置通知
                    String after = this.aopConfig.getAspectAfter();
                    if (aspectMethodMap.containsKey(after)) {
                        advices = adviceMaps.get(after);
                        if(advices == null) advices = new ArrayList<TAdvice>();
                        advices.add(new TAdvice(instance, aspectMethodMap.get(after)));
                        adviceMaps.put(after, advices);
                    }
                    //异常通知
                    String afterThrowing = this.aopConfig.getAspectAfterThrowing();
                    if (aspectMethodMap.containsKey(afterThrowing)) {
                        advices = adviceMaps.get(afterThrowing);
                        if(advices == null) advices = new ArrayList<TAdvice>();
                        advices.add(new TAdvice(instance, aspectMethodMap.get(afterThrowing)));
                        adviceMaps.put(afterThrowing, advices);
                    }
                    //4.存入缓存
                    this.methodAdvicesCache.put(method, adviceMaps);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
        //拿到原始类后需要解析出切面方法 Map<Method, Map<String, List<TAdvice>>>
        //原始方法对应一个切面通知 比如before对应多个List<Advice>
        parse();
    }


    public Map<String, List<TAdvice>> getMethodAdvices(Method method) throws NoSuchMethodException {
        Map<String, List<TAdvice>> cache = this.methodAdvicesCache.get(method);
        if (this.methodAdvicesCache.get(method) == null) {
            Method method1 = this.targetClass.getMethod(method.getName(), method.getParameterTypes());
            cache = this.methodAdvicesCache.get(method1);
            this.methodAdvicesCache.put(method, cache);
        }
        if (cache == null) {
            return new HashMap<String, List<TAdvice>>();
        }
        return cache;
    }
}
