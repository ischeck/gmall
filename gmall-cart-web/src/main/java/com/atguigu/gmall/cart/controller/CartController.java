package com.atguigu.gmall.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.handler.CartCookieHandler;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Controller
public class CartController {


        @Reference
        ManagerService managerService;

        @Reference
        CartService cartService;

        @Autowired
        CartCookieHandler cartCookieHandler;

        @LoginRequire(autoRedirect = false)
        @RequestMapping(value = "addToCart",method = RequestMethod.POST)
        public String addCart(HttpServletRequest request, HttpServletResponse response){
            String skuId = request.getParameter("skuId");
            Integer num=Integer.parseInt( request.getParameter("num"));

            SkuInfo skuInfo = managerService.getSkuInfo(skuId);
            if(skuInfo==null){
                request.setAttribute("errorMsgSelf","该商品或已下架");
                return "cartError";
            }
            String userId=(String)request.getAttribute("userId");
            if(userId!=null){
                cartService.addToCart(userId,skuInfo,num);
            }else{
                cartCookieHandler.addToCart(skuInfo, num ,request,response);
            }
            request.setAttribute("skuInfo",skuInfo);
            request.setAttribute("num",num);
            return "success";
        }


    /**
     *
     * springmvc controller中返回值类型为void的方法，需有一个response入参，向页面输出值
     * 如果没有，则会视该方法映射路径为页面进行渲染
     */
        @RequestMapping("getNum")
        public void getNum(HttpServletRequest request,HttpServletResponse response){
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            for (CartInfo info:cartList) {
                System.out.print("name="+info.getSkuName());
                System.out.print("num="+info.getSkuNum());

            }
            try {
                response.getWriter().print("<h2>write</h2>");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        String userId =(String) request.getAttribute("userId");
        List<CartInfo> cartCookieList = cartCookieHandler.getCartList(request);
        if(userId==null){//用户未登录，从cookie中取
            request.setAttribute("cartList",cartCookieList );
        }else {
            if(cartCookieList==null||cartCookieList.size()==0) {
                List<CartInfo> cartList = cartService.getCartList(userId);
                request.setAttribute("cartList",cartList );
            }else {//用户已登录，且cookie中有数据时，合并购物车
                List<CartInfo> cartList = cartService.mergeToCart(userId, cartCookieList);
                cartCookieHandler.deleteCartCookie(request,response);
                request.setAttribute("cartList",cartList );

            }
        }

        return "cartList";
    }


    @RequestMapping(value = "checkCart",method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("isChecked");
        if(userId!=null){
            cartService.setCheckedCart(userId,skuId,isChecked);
        }else{
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
        return ;
    }

    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        String userId =(String) request.getAttribute("userId");
        List<CartInfo> cartCookieList = cartCookieHandler.getCartList(request);
        if(cartCookieList!=null&&cartCookieList.size()>0) {
            List<CartInfo> cartList = cartService.mergeToCart(userId, cartCookieList);
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }


}
