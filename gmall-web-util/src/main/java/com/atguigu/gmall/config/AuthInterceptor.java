package com.atguigu.gmall.config;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //渲染昵称 对普通请求，渲染昵称在页面上。
        String token = CookieUtil.getCookieValue(request, "token", false);
        if(token!=null){
             Map userMap=getUserMapByToken(token);
            request.setAttribute("nickName",userMap.get("nickName"));
        }
        String currentUrl=request.getRequestURL().toString() ;//test使用
        //存入token 验证结束后的请求，存token入cookie
        String newToken = request.getParameter("newToken");
        //把token保存到cookie
        if(newToken!=null){
            CookieUtil.setCookie(request,response,"token",newToken,WebConst.COOKIE_MAXAGE,false);
            Map userMap=getUserMapByToken(newToken);
            request.setAttribute("nickName",userMap.get("nickName"));
            request.setAttribute("userId",userMap.get("userId"));
            return true;
        }

         //验证用户身份、验证是否已登录 对有验证需求的请求
                HandlerMethod handlerMethod =(HandlerMethod) handler;
                LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
                if(methodAnnotation!=null){//有验证需求
                    String originIp = request.getHeader("x-forwarded-for");

                    Map map=new HashMap();
                    map.put("originIp",originIp);
                    map.put("token",token);
                    String result=null;
                    if(token !=null) {//存在token，去验证
                       result = HttpClientUtil.doGet(WebConst.VERIFY_URL + "?token=" + token + "&originIp=" + originIp);
                    }
                    if(result!=null&&result.equals("success")){//验证成功，赋值userId
                        String userId=getUserMapByToken(token).get("userId");
                        request.setAttribute("userId",userId); //只有验证过才能取到userId
                        return true;
                    }else{//没有token或验证失败，若需要自动跳转登录页面去登录
                        if(methodAnnotation.autoRedirect()) {
                            String originUrl=request.getRequestURL().toString();
                            response.sendRedirect(WebConst.LOGIN_URL + "?originUrl=" + URLEncoder.encode(originUrl, "UTF-8"));
                            return false;
                        }
                    }
                }


        return true;

    }

    private Map<String,String> getUserMapByToken(String  token){
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map<String,String> map = JSON.parseObject(tokenJson, Map.class);
        return map;
    }

}
