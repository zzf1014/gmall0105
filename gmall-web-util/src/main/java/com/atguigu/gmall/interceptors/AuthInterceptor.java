package com.atguigu.gmall.interceptors;

import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{


        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

            // 拦截代码

            //判断被拦截的请求的方法的注解（是否需要被拦截）
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            LoginRequired methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequired.class);
            if (methodAnnotation == null){ // 不存在注解 不需要拦截
                return  true;
            }

            boolean loginSuccess = methodAnnotation.loginSuccess(); //获得该请求是否必须登陆成功

            System.out.println("进入拦截器的拦截方法");

            return true;
        }
}
