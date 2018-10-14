package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;
    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    PaymentService paymentService;

    @RequestMapping(value = "index",method = RequestMethod.GET)
    public String paymentIndex(@RequestParam("orderId")String  orderId, Model model){

        OrderInfo orderInfo=orderService.getOrderInfo( orderId );

        model.addAttribute("orderId",orderInfo.getId());
        model.addAttribute("totalAmount",orderInfo.getTotalAmount());

        return "paymentIndex";
    }

    @RequestMapping(value = "/alipay/submit",method = RequestMethod.POST)
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        //1    用订单号查询订单详情
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        //2 保存支付信息

        PaymentInfo paymentInfo=new PaymentInfo();

        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderId);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getOrderSubject());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);

        paymentService.savePaymentInfo(paymentInfo);
        //4 制作支付宝参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);

        Map<String,Object> bizContnetMap=new HashMap<>();
        bizContnetMap.put("out_trade_no",paymentInfo.getOutTradeNo());
        bizContnetMap.put("subject",paymentInfo.getSubject());
        bizContnetMap.put("total_amount",paymentInfo.getTotalAmount());
        bizContnetMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        String bizContent = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(bizContent);
        String form=null;
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8" );
        //发起延时队列 查询支付状况
        paymentService.sendDelayPaymentResultCheck(paymentInfo.getOutTradeNo(),15,3);
        return form;

    }

    @RequestMapping(value="/alipay/callback/notify",method = RequestMethod.POST)
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String,String> paramMap) {
        System.out.println(" ----------callbackstart 支付宝开始回调"+paramMap.toString() );
        //验证签名
        boolean isCheckPass=false;
        try {
            isCheckPass = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(!isCheckPass){
            System.out.println(" ----------验签不通过！！"  );
            return "验签不通过！！";
        }
        System.out.println(" ----------验签通过！！"  );
        //验证成功标志
        String trade_status = paramMap.get("trade_status");
        if("TRADE_SUCCESS".equals(trade_status)){
            //检查当前支付状态
            String outTradeNo = paramMap.get("out_trade_no");
            PaymentInfo paymentInfo = paymentService.getPaymentInfoByOutTradeNo(outTradeNo);
            if (paymentInfo==null) {
                return "error: not exists out_trade_no:"+outTradeNo;
            }
            System.out.println("检查是否已处理= " +outTradeNo  );
            if(paymentInfo.getPaymentStatus()==PaymentStatus.PAID){
                //如果已经处理过了 就直接返回成功标志
                System.out.println(" 已处理= " +outTradeNo  );
                return "success";
            }else {
                //先更新支付状态
                System.out.println(" 未处理，更新状态= " +outTradeNo  );
                PaymentInfo paymentInfo4Upt=new PaymentInfo();
                paymentInfo4Upt.setPaymentStatus(PaymentStatus.PAID);
                paymentInfo4Upt.setCallbackTime(new Date());
                paymentInfo.setCallbackContent(paramMap.toString());
                paymentService.updatePaymentInfoByOutTradeNo(outTradeNo,paymentInfo4Upt);

                //发送通知给订单
                sendPaymentResult(paymentInfo,"success");
                return  "success";

            }

        }
        return "";

    }


    @RequestMapping(value="/alipay/callback/return",method = RequestMethod.GET)
    public String callbackReturn(HttpServletRequest request, Model model) throws UnsupportedEncodingException {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++同步回调至此");

        return  "redirect:"+AlipayConfig.return_order_url;
    }

    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result") String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }
}
