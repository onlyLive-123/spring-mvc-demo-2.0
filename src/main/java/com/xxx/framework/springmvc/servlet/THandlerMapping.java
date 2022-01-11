package com.xxx.framework.springmvc.servlet;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class THandlerMapping {

    Object instance;
    Method method;
    String url;

    public THandlerMapping(Object instance, Method method, String url) {
        this.instance = instance;
        this.method = method;
        this.url = url;
    }
}
