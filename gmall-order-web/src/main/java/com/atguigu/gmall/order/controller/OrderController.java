package com.atguigu.gmall.order.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManagerService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserManagerService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

        @Reference
        private UserManagerService userManagerService;

        @Reference
        private CartService cartService;

        @Reference
        private OrderService orderService;

        @Reference
        private ManagerService managerService;





    @RequestMapping("/isOk")
        @ResponseBody
        public String testOk(){
              String result=  userManagerService.sayOk();
            return "result ="+result;

        }


    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest httpServletRequest){
        String userId = (String) httpServletRequest.getAttribute("userId");
        //获取用户地址列表
        List<UserAddress> userAddressList = userManagerService.getUserAddressList(userId);
        httpServletRequest.setAttribute("userAddressList",userAddressList);
        //获取用户勾选的商品列表
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);
        //构造订单详情信息
        List<OrderDetail> orderDetailList=new ArrayList<>(cartCheckedList.size());
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail=new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetailList.add(orderDetail);
        }
        httpServletRequest.setAttribute("orderDetailList",orderDetailList);
        //构造订单信息
        OrderInfo orderInfo=new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        httpServletRequest.setAttribute("totalAmount",orderInfo.getTotalAmount());

        //maybe 防止重复提交
        String tradeCode = orderService.genTradeCode(userId);
        httpServletRequest.setAttribute("tradeCode",tradeCode);

        return "trade";
    }


    @RequestMapping("submitOrder")
    @LoginRequire
    public  String submitOrder(OrderInfo orderInfo, HttpServletRequest request){

        String userId =(String) request.getAttribute("userId");
        //0 检查tradeCode
        String tradeCode = request.getParameter("tradeCode");
        boolean existsTradeCode = orderService.checkTradeCode(userId, tradeCode);
        if(!existsTradeCode){
            request.setAttribute("errMsg","该页面已失效，请重新结算！");
            return "tradeFail";
        }

        //1 构建orderInfo

        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);
        //2 校验
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        for (OrderDetail orderDetail : orderDetailList) {
            //验价
            SkuInfo skuInfo = managerService.getSkuInfo(orderDetail.getSkuId());
            if(!skuInfo.getPrice().equals( orderDetail.getOrderPrice())){
                request.setAttribute("errMsg","您选择的商品可能存在价格变动，请重新下单。");
                cartService.loadCartCache(userId);//更新购物车商品价格
                return "tradeFail";
            }
            //验库存
            boolean hasStock = checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if(!hasStock){
                request.setAttribute("errMsg","您的商品【"+orderDetail.getSkuName()+"】库存不足，请重新下单。。");
                return "tradeFail";
            }
        }


        //3  保存
        String orderId = orderService.saveOrder(orderInfo);
        orderService.delTradeCode(userId);
        //4 重定向
        return "redirect://payment.gmall.com/index?orderId="+orderId;

    }


    private boolean checkStock(String skuId,Integer skuNum){
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if("1".equals(result)){
            return true;
        }
        return false;
    }

    @RequestMapping(value = "orderSplit",method = RequestMethod.POST)
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        final OrderService orderService = this.orderService;
        List<OrderInfo> subOrderInfoList= orderService.splitOrder(orderId,wareSkuMap);
        List<Map> wareMapList=new ArrayList<>();
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map wareMap = orderService.initWareOrder(orderInfo);
            wareMapList.add(wareMap);
        }

        String jsonString = JSON.toJSONString(wareMapList);
        return jsonString;
    }

}
