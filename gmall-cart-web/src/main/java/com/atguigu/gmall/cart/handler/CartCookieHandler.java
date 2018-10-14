package com.atguigu.gmall.cart.handler;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.config.WebConst;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

   @Component
    public class CartCookieHandler {

        String cartCookieName="CART";

        public void addToCart(SkuInfo cartSkuInfo, Integer num, HttpServletRequest request,
                              HttpServletResponse response) {
            try {
                List<CartInfo> cartList = getCartList(request);

                CartInfo cart = null;
                if (cartList != null && cartList.size() > 0) {
                    for (CartInfo c : cartList) {
                        // 判断购物车中是否存在该商品
                        if (c.getSkuId().equals(cartSkuInfo.getId())) {
                            cart = c;
                            break;
                        }
                    }
                }

                if (cart == null) {
                    // 当前的购物车没有该商品
                    cart = new CartInfo();
                    cart.setSkuId(cartSkuInfo.getId());
                    cart.setSkuName(cartSkuInfo.getSkuName());
                    // 设置商品主图
                    cart.setImgUrl(cartSkuInfo.getSkuDefaultImg());
                    cart.setSkuPrice(cartSkuInfo.getPrice());
                    cart.setCartPrice(cartSkuInfo.getPrice());
                    cart.setSkuNum(num);

                    cartList.add(cart);
                } else {
                    // 在购物车中存在该商品
                    cart.setSkuNum(cart.getSkuNum() + num);
                }
                // 设置购物车的商品，过期时间7天
                CookieUtil.setCookie(request, response, cartCookieName, JSON.toJSONString(cartList), WebConst.COOKIE_MAXAGE,true);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public List<CartInfo> getCartList(HttpServletRequest request) {

            try {
                String cartListJson = CookieUtil.getCookieValue(request,cartCookieName , true);
                if (cartListJson!= null) {
                    List<CartInfo> cartList = JSON.parseArray(cartListJson, CartInfo.class);
                    return cartList;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }


       public void deleteCartCookie(HttpServletRequest request,
                                    HttpServletResponse response){
           CookieUtil.deleteCookie(request,response,cartCookieName);
       }


       public void checkCart(HttpServletRequest request, HttpServletResponse response,String skuId,String isChecked){
           // 1 取出所有购物车中的商品列表
           List<CartInfo> cartList = getCartList(request);

           // 2 循环比较skuId
           for (CartInfo cartInfo : cartList) {
               if(  cartInfo.getSkuId().equals(skuId)){
                   // 3 如果匹配赋上isChecked
                   cartInfo.setIsChecked(isChecked);
               }
           }


           // 4保存进cookie
           String newCartJson = JSON.toJSONString(cartList);
           CookieUtil.setCookie(request,response,cartCookieName,newCartJson,WebConst.COOKIE_MAXAGE,true);

       }

   }


