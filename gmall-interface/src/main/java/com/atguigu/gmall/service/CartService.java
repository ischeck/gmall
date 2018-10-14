package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;

import java.util.List;

public interface CartService {
    void addToCart(String userId, SkuInfo skuInfo, Integer skuNum);
    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCart(String userId, List<CartInfo> cartCookieList);

    void setCheckedCart(String userId,String skuId,String isChecked);

    List<CartInfo> getCartCheckedList(String userId);

    List<CartInfo> loadCartCache(String userId);
}
