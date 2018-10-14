package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfoByOutTradeNo(String outTradeNo);

    void updatePaymentInfoByOutTradeNo(String outTradeNo , PaymentInfo paymentInfo);

    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    boolean checkPayment(PaymentInfo paymentInfo);

    void sendDelayPaymentResultCheck(String outTradeNo, int delaySec, int i);

    void closePayment(String id);
}
