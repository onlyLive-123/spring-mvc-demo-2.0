package com.xxx.framework;

import com.xxx.demo.action.HelloAction;
import com.xxx.framework.spring.content.TestApplicationContext;

public class Test {

    public static void main(String[] args) {
        TestApplicationContext context = new TestApplicationContext("classPath:application.properties");
        HelloAction action = (HelloAction) context.getBean(HelloAction.class);
        String hello = action.hello("张三");
        System.out.println(hello);
    }

}
