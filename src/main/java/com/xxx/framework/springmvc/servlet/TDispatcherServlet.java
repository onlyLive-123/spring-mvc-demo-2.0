package com.xxx.framework.springmvc.servlet;

import com.xxx.framework.Model.TModelAndView;
import com.xxx.framework.annotation.TController;
import com.xxx.framework.annotation.TRequestMapping;
import com.xxx.framework.beans.config.TBeanDefinition;
import com.xxx.framework.spring.content.TestApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class TDispatcherServlet extends HttpServlet {

    private TestApplicationContext context;

    //spring 源码复制出来的 为什么不用map 可能不止用url请求 源码用的是遍历
    //通过urlPath 拿到 THandlerMapping
    private List<THandlerMapping> handlerMappings = new ArrayList<THandlerMapping>();
//    Map<String,THandlerMapping> hadlerMappingMap = new HashMap<String, THandlerMapping>();

    private List<THandlerAdapter> handlerAdapters = new ArrayList<THandlerAdapter>();
    //spring采用的是list 这里简化吧
    private TViewResolver viewResolver;
//    private List<TViewResolver> viewResolvers = new ArrayList<TViewResolver>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //调用
            doDispatcher(req, resp);
        } catch (Exception e) {
            //404的 直接返回了 这里的异常全是服务器500异常
            Map<String, Object> modelMap = new HashMap<String, Object>();
            modelMap.put("msg", e.getMessage());
            modelMap.put("error", Arrays.toString(e.getStackTrace()));
            try {
                processDispatchResult(req, resp, new TModelAndView("500", modelMap));
            } catch (Exception e1) {
                e1.printStackTrace();
                resp.getWriter().write("500 Exception,Detail : " + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //先匹配url拿到mapping
        THandlerMapping handler = getHandler(req);
        if (handler == null) {
            processDispatchResult(req, resp, new TModelAndView("404"));
            return;
        }
        //从这里开始 异常都是 服务器异常了
        //根据mapping到HandlerAdapter 取回参数处理器
        THandlerAdapter ha = getHandlerAdapter(handler);

        //处理参数 反射调用
        TModelAndView mv = ha.handle(req, resp, handler);

        //返回值处理 是否返回页面
        processDispatchResult(req, resp, mv);
    }

    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, TModelAndView mv) throws Exception {
        if(mv != null){
            TView view = viewResolver.resolveViewName(mv.getHtmlName());
            //渲染
            view.render(req,resp,mv.getModel());
        }
    }

    private THandlerAdapter getHandlerAdapter(THandlerMapping handler) {
        for (THandlerAdapter adapter : this.handlerAdapters) {
            if (adapter.handlerMapping.equals(handler)) {
                return adapter;
            }
        }
        return null;
    }

    private THandlerMapping getHandler(HttpServletRequest req) {
        String reqUrl = req.getRequestURI().replaceAll(req.getContextPath(), "");
        for (THandlerMapping handlerMapping : this.handlerMappings) {
            if (handlerMapping.getUrl().equals(reqUrl)) { //如果是正则 这里直接正则匹配
                return handlerMapping;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //IOC DI阶段 直接调用上下文
        this.context = new TestApplicationContext(
                config.getInitParameter("contextConfigLocation"));

        //2.参考springMvc DispatcherServlet 初始化9大组件
        initStrategies(context);

        System.out.println("spring mvc is init");
    }

    //源码里复制
    private void initStrategies(TestApplicationContext context) {
//        //多文件上传的组件
//        this.initMultipartResolver(context);
//        //初始化本地语言环境
//        this.initLocaleResolver(context);
//        //初始化模板处理器
//        this.initThemeResolver(context);
        //url请求
        this.initHandlerMappings(context);
        //初始化参数适配器
        this.initHandlerAdapters(context);
//        //初始化异常拦截器
//        this.initHandlerExceptionResolvers(context);
//        //初始化视图预处理器
//        this.initRequestToViewNameTranslator(context);
        //初始化视图转换器
        this.initViewResolvers(context);
//        //FlashMap管理器
//        this.initFlashMapManager(context);
    }

    private void initViewResolvers(TestApplicationContext context) {
        //初始化模板的位置
        URL url = this.getClass().getClassLoader().getResource(context.getConfig().getProperty("htmlRootTemplate"));
        this.viewResolver = new TViewResolver(url.getPath());
    }

    private void initHandlerAdapters(TestApplicationContext context) {
        //初始化参数的位置
        for (THandlerMapping handlerMapping : this.handlerMappings) {
            //通过构造方法初始化 初始化时处理好参数
            this.handlerAdapters.add(new THandlerAdapter(handlerMapping));
        }
    }

    private void initHandlerMappings(TestApplicationContext context) {
        //从IOC容器中拿到实例化对象 遍历方法找到TRequestMapping 注解 取到URL
        Map<String, TBeanDefinition> beanDefinitionMap = context.getBeanDefinitionMap();
        for (String beanName : beanDefinitionMap.keySet()) {
            Object instance = context.getBean(beanName);
            Class<?> clazz = instance.getClass();
            //只有加了TController注解才扫描
            if (!clazz.isAnnotationPresent(TController.class)) continue;
            //类路径
            String baseUrl = "";
            if (clazz.isAnnotationPresent(TRequestMapping.class)) {
                TRequestMapping baseMapping = clazz.getAnnotation(TRequestMapping.class);
                if (!"".equals(baseMapping.value())) {
                    baseUrl = baseMapping.value();
                }
                //获取所有的方法 扫描注解
                for (Method method : clazz.getMethods()) {
                    if (!method.isAnnotationPresent(TRequestMapping.class)) continue;

                    TRequestMapping mapping = method.getAnnotation(TRequestMapping.class);
                    if ("".equals(mapping)) {
                        throw new RuntimeException("the method " + method.toString() + " TRequestMapping.value is null!");
                    }
                    //如果请求路径想要用到正则 将requrl转成 正则匹配即可
                    String reqUrl = (baseUrl + "/" + mapping.value()).replaceAll("/+", "/");

                    //保存对应关系 原始类对象 方法对象 reqUrl 后面invoke调用
                    handlerMappings.add(new THandlerMapping(instance, method, reqUrl));
                    System.out.println("添加访问路径:" + reqUrl);
                }
            }
        }
    }


}
