package com.atguigu.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserManagerService;
import com.atguigu.gmall.passport.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserManagerService userManagerService;


    @Value("${token.key}")
    String TOKEN_KEY;

    @RequestMapping("index")
    public String index(HttpServletRequest httpServletRequest){
        String originUrl=httpServletRequest.getParameter("originUrl");
        httpServletRequest.setAttribute("originUrl",originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest httpServletRequest){
        String originIp = httpServletRequest.getHeader("x-forwarded-for");
        UserInfo user = userManagerService.login(userInfo);
        if (user==null){
            return "fail";
        }else {//若用户存在，跳转回原页面并携带token
            Map map=new HashMap();
            map.put("userId",user.getId());
            map.put("nickName",user.getNickName());

            String token = JwtUtil.encode(TOKEN_KEY, map, originIp);
            return token;

        }

    }

    @RequestMapping(value ="verify")
    @ResponseBody
    public String verify(HttpServletRequest httpServletRequest){
        String token = httpServletRequest.getParameter("token");
        String originIp=httpServletRequest.getParameter("originIp");
        Map<String, Object> map = JwtUtil.decode(token, TOKEN_KEY, originIp);
        if (map!=null){//已确认身份，核对是否登录
            String userId =(String) map.get("userId");
            boolean verify = userManagerService.verify(userId);
            if(verify){
                return "success";
            }
        }

        return "fail";


    }


}
