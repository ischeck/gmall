package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Service
public class OrderServiceImpl  implements OrderService{


    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;
    @Reference
    private PaymentService paymentService;


    public String saveOrder(OrderInfo orderInfo) {
        //构建orderInfo
        orderInfo.setCreateTime(new Date());

        Calendar calendar= Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());

        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //存入orderInfo
        orderInfoMapper.insertSelective(orderInfo);

        //存入orderDetail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId( orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        return orderInfo.getId();
    }

    public String genTradeCode(String userId){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode= UUID.randomUUID().toString();
        jedis.setex(tradeNoKey, 10 * 60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    public boolean checkTradeCode(String userId,String tradeCodePage){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
        if(tradeCode!=null&&tradeCode.equals(tradeCodePage)){
            return true;
        }else{
            return false;
        }
    }


    public OrderInfo getOrderInfo(String orderId){
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail  orderDetailQuery=new OrderDetail();
        orderDetailQuery.setOrderId(orderId);

        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetailQuery);

        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;


    }



    public void updateOrderStatus(String orderId , ProcessStatus processStatus, Map<String,String>... paramMaps) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfo.setProcessStatus(processStatus);

        //动态增加需要补充更新的属性
        if (paramMaps != null && paramMaps.length > 0) {
            Map<String, String> paramMap = paramMaps[0];
            for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                String properties = entry.getKey();
                String value = entry.getValue();
                try {
                    BeanUtils.setProperty(orderInfo, properties, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }



    public void delTradeCode(String userId){
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();
    }


    public String initWareOrderMessage(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }


    public Map  initWareOrder(OrderInfo orderInfo){

        Map map=new HashMap();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getOrderSubject());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());

        List detailList=new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map  detailMap =new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());

            detailList.add(detailMap);
        }

        map.put("details",detailList);

        return  map;

    }


    /**
     * 通知库存
     * @param orderId
     */
    public void sendOrderResult(String orderId){
        String orderJson = initWareOrderMessage(orderId);
        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue orderStatusQueue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(orderStatusQueue);

            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText(orderJson);

            producer.send(textMessage);

            session.commit();

            session.close();
            producer.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }


    }

    public List<OrderInfo> getExpiredOrderList(){
        Example example=new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime",new Date()).andEqualTo("processStatus",ProcessStatus.UNPAID);


        List<OrderInfo> orderInfos = orderInfoMapper.selectByExample(example);
        return orderInfos;
    }

    @Async
    public void execExpiredOrder(OrderInfo orderInfo){
        updateOrderStatus(orderInfo.getId(), ProcessStatus.CLOSED);
        paymentService.closePayment(orderInfo.getId());
    }


    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList=new ArrayList<>();

        //1、先查询出原始订单信息
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);

        //2 wareSkuMap 反序列化
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);

        //3 遍历拆单方案  每个仓库与商品的对照 形成一个子订单
        for (Map map : maps) {
            String wareId = (String)map.get("wareId");
            List<String> skuIds = (List)map.get("skuIds");
            //4  生成子订单主表  从原始订单复制    新的订单号  父订单
            OrderInfo subOrderInfo =new OrderInfo();

            try {
                BeanUtils.copyProperties(subOrderInfo,orderInfoOrigin);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
            subOrderInfo.setWareId(wareId);

            //5 原始订单 订单主表中的订单状态标志为拆单


            //6 明细表  根据拆单方案中的skuids进行匹配 到不同的子订单
            List<OrderDetail> subOrderDetailList=new ArrayList<>();
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            for (String skuId : skuIds) {
                for (OrderDetail orderDetail : orderDetailList) {
                    if(skuId.equals(orderDetail.getSkuId())){
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }

            subOrderInfo.setOrderDetailList(subOrderDetailList);

            subOrderInfo.sumTotalAmount();
            //7 保存到数据库中。
            saveOrder(subOrderInfo);

            subOrderInfoList.add(subOrderInfo);


        }

        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        //       8 返回一个 新生成的子订单列表
        return subOrderInfoList;
    }


}
