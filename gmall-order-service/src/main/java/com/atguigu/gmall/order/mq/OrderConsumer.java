package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {

        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println("result = " + result);
        System.out.println("orderId = " + orderId);
        if("success".equals(result)){
            //更新订单状态为 已支付
            orderService.updateOrderStatus(  orderId, ProcessStatus.PAID);
            //通知仓库系统
          orderService.sendOrderResult(orderId);
          //更新订单状态为 已通知仓库
            orderService.updateOrderStatus(  orderId,   ProcessStatus.NOTIFIED_WARE);
        }

    }

    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeDeductQueue(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        if("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(  orderId,   ProcessStatus.WAITING_DELEVER);
        }else{
            orderService.updateOrderStatus(  orderId,   ProcessStatus.STOCK_EXCEPTION);
        }

    }

}
