//package com.changgou.goods.intercept;
//
//import feign.RequestInterceptor;
//import feign.RequestTemplate;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.Enumeration;
//
///**
// * author:JiangSong
// * Date:2023/7/20
// *
// *
// * Feign拦截器，在执行之前将管理员Token封装进头信息
// *
// * 已经封装到工具类中
// **/
//
//@Configuration
//public class TokenRequestInterceptor implements RequestInterceptor {
//    // 获取用户令牌信息并封装到请求头
//    @Override
//    public void apply(RequestTemplate requestTemplate) {
//        try {
//            //使用RequestContextHolder工具获取request相关变量
//            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//            if (attributes != null) {
//                //取出request
//                HttpServletRequest request = attributes.getRequest();
//                //获取所有头文件信息的key
//                Enumeration<String> headerNames = request.getHeaderNames();
//                if (headerNames != null) {
//                    while (headerNames.hasMoreElements()) {
//                        //头文件的key
//                        String name = headerNames.nextElement();
//                        //头文件的value
//                        String values = request.getHeader(name);
//                        //将令牌数据添加到头文件中
//                        requestTemplate.header(name, values);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//}
