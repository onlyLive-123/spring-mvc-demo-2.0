package com.xxx.framework.springmvc.servlet;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class TView {

    File htmlFile;  //对应的html文件

    public TView(File htmlFile) {
        this.htmlFile = htmlFile;
    }

    public void render(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> model) throws Exception {
        RandomAccessFile ra = new RandomAccessFile(htmlFile, "r");
        StringBuffer sb = new StringBuffer();
        String line = null;
        while (null != (line = ra.readLine())) {
            line = new String(line.getBytes("ISO-8859-1"), "utf-8");
            Pattern pattern = Pattern.compile("#\\{[^\\}]+\\}", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String paramName = matcher.group();
                paramName = paramName.replaceAll("#\\{|\\}", "");
                Object paramValue = model.get(paramName);
                if (paramValue != null) {
                    line = matcher.replaceFirst(makeStringForRegExp(paramValue.toString()));
                } else {
                    line = matcher.replaceFirst("null");
                }
                matcher = pattern.matcher(line);
            }
            sb.append(line);
        }
        resp.setCharacterEncoding("utf-8");
        resp.getWriter().write(sb.toString());
    }

    //处理特殊字符
    public static String makeStringForRegExp(String str) {
        return str.replace("\\", "\\\\").replace("*", "\\*")
                .replace("+", "\\+").replace("|", "\\|")
                .replace("{", "\\{").replace("}", "\\}")
                .replace("(", "\\(").replace(")", "\\)")
                .replace("^", "\\^").replace("$", "\\$")
                .replace("[", "\\[").replace("]", "\\]")
                .replace("?", "\\?").replace(",", "\\,")
                .replace(".", "\\.").replace("&", "\\&");
    }
}
