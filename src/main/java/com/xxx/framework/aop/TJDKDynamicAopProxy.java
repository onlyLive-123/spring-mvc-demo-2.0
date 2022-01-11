package com.xxx.framework.aop;

import com.xxx.framework.Model.JoinPoint;
import com.xxx.framework.aop.aspect.TAdvice;
import com.xxx.framework.aop.support.TAdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理类走向
 */
public class TJDKDynamicAopProxy implements InvocationHandler {

    private TAdvisedSupport config;

    public TJDKDynamicAopProxy(TAdvisedSupport advisedSupport) {
        this.config = advisedSupport;
    }

    /**
     * 代理会走到这个方法
     * 1.获取aspect AopConfig配置,切面信息和原始实例类
     * 2.保存method -> list<TAopConfig>配置 到map 可能多个切面
     * 3.依次调用 invoke
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        Map<String, List<TAdvice>> advicesMap = new HashMap<String, List<TAdvice>>();

        Object result = null;
        JoinPoint joinPoint = new JoinPoint();
        joinPoint.setArgs(args);
        joinPoint.setTarget(config.getTarget());
        joinPoint.setMethod(method);
        try {
            advicesMap = config.getMethodAdvices(method);

            invokeAspect(advicesMap.get(config.getAopConfig().getAspectBefore()), joinPoint);

            result = method.invoke(config.getTarget(), args);

            joinPoint.setResult(result);
            invokeAspect(advicesMap.get(config.getAopConfig().getAspectAfter()), joinPoint);
        } catch (Exception e) {
            joinPoint.setThrowName(e.getCause().getMessage());
            invokeAspect(advicesMap.get(config.getAopConfig().getAspectAfterThrowing()), joinPoint);
            throw e;
        }
        return result;
    }

    private void invokeAspect(List<TAdvice> advices, JoinPoint joinPoint) {
        try {
            if (advices == null || advices.size() == 0) return;
            for (TAdvice advice : advices) {
                joinPoint.setChainTarget(advice.getAspectClass());
                joinPoint.setChainMethod(advice.getAspectMethod());
                joinPoint.proceed();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * JDK通过接口创建代理对象 无接口的暂时不考虑
     *
     * @return
     */
    public Object getProxy() {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                this.config.getTargetClass().getInterfaces(), this);
    }
}
