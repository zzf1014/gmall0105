package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{


        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

            // 拦截代码

            //判断被拦截的请求的方法的注解（是否需要被拦截）
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            LoginRequired methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequired.class);

            // 是否拦截
            if (methodAnnotation == null){ // 不存在注解 不需要拦截
                return  true;
            }
            String token = "";
            String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
            if (StringUtils.isNotBlank(oldToken)){ // 之前登陆过
                token = oldToken;
            }
            String newToken = request.getParameter("token");
            if (StringUtils.isNotBlank(newToken)){ // 刚刚登陆
                token = newToken;
            }
            // 是否必须登陆
            boolean loginSuccess = methodAnnotation.loginSuccess(); //获得该请求是否必须登陆成功

            // 调用认证中心 验证   之所以把认证中心写成web而不是service是因为service的话之能允许此项目调用，不利于其它调用
            String success = "fail";
            Map<String,String> successMap = new HashMap<>();
            if (StringUtils.isNotBlank(token)){
                // 获取ip
                String ip = request.getHeader("x-forwarded-for"); // 通过nginx转发的客户端ip
                if (StringUtils.isBlank(ip)){
                    ip = request.getRemoteAddr(); // 从request中取IP
                    if (StringUtils.isBlank(ip)){
                        ip = "127.0.0.1";
                    }
                }
               String successJson = HttpclientUtil.doGet("http://127.0.0.1:8085/verify?token=" + token+"&currentIp="+ip);// token对还是错
               successMap = JSON.parseObject(success, Map.class);
                success = successMap.get("status");
            }


            if (loginSuccess){  // 必须登陆
               if (!success.equals("success")){
                   // 验证失败 重定向到passport登陆
                   StringBuffer requestURL = request.getRequestURL();
                   response.sendRedirect("http://127.0.0.1:8085/index?ReturnUrl="+requestURL);
                   return false;
               }else {

                   // 需要将token携带的用户信息 写入
                   request.setAttribute("memberId",successMap.get("memberId"));
                   request.setAttribute("nickname",successMap.get("nickname"));
                   return  true;
               }
            }else{
                // 没有登陆也能用，必须验证
                if (success.equals("success")){
                    // 需要将token携带的用户信息 写入
                    request.setAttribute("memberId",successMap.get("memberId"));
                    request.setAttribute("nickname",successMap.get("nickname"));
                }
            }
            // 验证通过 覆盖cookie的token
            if (StringUtils.isNotBlank(token)){
                CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
            }

            return true;
        }
}
