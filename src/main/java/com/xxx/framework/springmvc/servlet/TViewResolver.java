package com.xxx.framework.springmvc.servlet;

import java.io.File;

/**
 * 页面文件准备
 */
public class TViewResolver {

    String templatePath;

    public TViewResolver(String filePath) {
        this.templatePath = filePath;
    }

    public TView resolveViewName(String htmlName) {
        //如果不带后缀 可以添加 spring里通过配置来的
        htmlName = this.templatePath + "/" + htmlName + (htmlName.endsWith(".html") ? "" : ".html");
        return new TView(new File(htmlName));
    }
}
