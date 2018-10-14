package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    String genTradeCode(String userId);

    String saveOrder(OrderInfo orderInfo);

    void delTradeCode(String userId);

    boolean checkTradeCode(String userId, String tradeCode);

    OrderInfo getOrderInfo(String orderId);

    void updateOrderStatus(String orderId , ProcessStatus processStatus, Map<String,String>... paramMaps);

    void sendOrderResult(String orderId);

    List<OrderInfo> getExpiredOrderList();

    void execExpiredOrder(OrderInfo orderInfo);

    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);

    Map initWareOrder(OrderInfo orderInfo);
}
