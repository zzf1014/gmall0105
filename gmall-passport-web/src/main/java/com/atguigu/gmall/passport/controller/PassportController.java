package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@CrossOrigin
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,HttpServletRequest request,String currentIp){
        Map<String,String> map = new HashMap<>();


        // 通过jwt校验token真假
        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", currentIp);
        if (decode!=null){
            map.put("status","success");
            map.put("memberId",(String)decode.get("memberId"));
            map.put("nickname",(String)decode.get("nickname"));
        }else{
            map.put("status","fail");
        }


        return JSON.toJSONString(map);
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap modelMap){
        modelMap.put("ReturnUrl",ReturnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request){

        // 调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        String token = "";
        if (umsMemberLogin!=null){
            // 登陆成功
            // jwt生成token
            Map<String,Object> userMap = new HashMap<>();
            String memberId = umsMember.getId();
            String nickname = umsMember.getNickname();
            userMap.put("memberId",memberId);
            userMap.put("nickname",nickname);

            // 获取ip
            String ip = request.getHeader("x-forwarded-for"); // 通过nginx转发的客户端ip
            if (StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr(); // 从request中取IP
                if (StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }

            // 安装设计的算法进行加密
             token = JwtUtil.encode("2019gmall0105", userMap, ip);
            // 将token存入redis一份
            userService.addUserToken(token,memberId);
        }else {
            // 登陆失败
            token = "fail";
        }
        return token;
    }
}
