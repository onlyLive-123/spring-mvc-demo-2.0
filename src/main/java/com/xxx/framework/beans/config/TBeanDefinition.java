package com.xxx.framework.beans.config;

import lombok.Data;

/**
 *  为实例化bean包装
 */
@Data
public class TBeanDefinition {

    String factoryBeanName;     //beanName
    String beanClassName;       //类名全路径
    String interfaceImplBeanName;       //如果是接口 对应实现类的beanNma

    public TBeanDefinition(String beanName, String beanClassName) {
        this.factoryBeanName = beanName;
        this.beanClassName = beanClassName;
    }

    public TBeanDefinition(String factoryBeanName, String beanClassName, String interfaceImplBeanName) {
        this.factoryBeanName = factoryBeanName;
        this.beanClassName = beanClassName;
        this.interfaceImplBeanName = interfaceImplBeanName;
    }
}
