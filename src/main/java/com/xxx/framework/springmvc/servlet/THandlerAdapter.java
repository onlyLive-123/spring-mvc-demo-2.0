package com.xxx.framework.springmvc.servlet;

import com.xxx.framework.Model.TModelAndView;
import com.xxx.framework.annotation.TRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 通过方法组成 参数->位置 的map
 */
public class THandlerAdapter {

    THandlerMapping handlerMapping;
    Method method;
    Class<?>[] parameterTypes;  //参数位置的类型
    Map<String, Integer> indexMap;   //参数的位置


    public THandlerAdapter(THandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
        this.method = handlerMapping.getMethod();
        this.parameterTypes = method.getParameterTypes();

        //运行阶段处理好参数位置
        doHandlerParams();
    }

    //参考源码里面的参数获取
    private void doHandlerParams() {
        this.indexMap = new HashMap<String, Integer>();

        //1.先解析 req 和 resp
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                this.indexMap.put(type.getName(), i);
            }
        }

        //2.解析带注解的 参数是二维数组 可能会有多个注解 多个参数
        Annotation[][] pas = method.getParameterAnnotations();
        for (int k = 0; k < pas.length; k++) {
            Annotation[] pa = pas[k];
            for (int i = 0; i < pa.length; i++) {
                Annotation annotation = pa[i];
                if (annotation instanceof TRequestParam) {
                    String name = ((TRequestParam) annotation).name();
                    this.indexMap.put(name, k);
                }
            }
        }
    }

    //组装参数进行调用 并返回
    public TModelAndView handle(HttpServletRequest req, HttpServletResponse resp, THandlerMapping handler) throws Exception {
        //获得前端传的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        Object[] params = new Object[this.indexMap.size()];

        for (Map.Entry<String, Integer> entry : this.indexMap.entrySet()) {
            String key = entry.getKey();
            Integer index = entry.getValue();
            //遍历参数位置 先 req和resp
            if (key.equals(HttpServletRequest.class.getName())) {
                params[index] = req;
            }
            if (key.equals(HttpServletResponse.class.getName())) {
                params[index] = resp;
            }

            //再是参数位置
            if (parameterMap.containsKey(key)) {
                //因为是个数组 可能会有多个值 需要处理成字符串
                String value = Arrays.toString(parameterMap.get(key)).replaceAll("\\[|\\]", "");
                //转义成和参数位置一致的类型
                params[index] = converter(value, this.parameterTypes[index]);
            }
        }
        //反射调用
        Object result = handler.getMethod().invoke(handler.getInstance(), params);
        if (result == null || result instanceof Void) return null;
        if (result instanceof TModelAndView) {
            return (TModelAndView) result;
        } else {
            resp.getWriter().write(result.toString());
        }
        return null;
    }

    //类型转换器
    private Object converter(String value, Class<?> type) {
        if (type == Integer.class) {
            return Integer.valueOf(value);
        } else if (type == Double.class) {
            return Double.valueOf(value);
        }
        return value;
    }
}
