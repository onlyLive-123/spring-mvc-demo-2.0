package com.xxx.framework.beans;

import lombok.Data;

@Data
public class TBeanWarpper {

    Object warpperInstance;
    Class<?> warpperClass;

    public TBeanWarpper(Object instance) {
        this.warpperInstance = instance;
        this.warpperClass = instance.getClass();
    }
}
