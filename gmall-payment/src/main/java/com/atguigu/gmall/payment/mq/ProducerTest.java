package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

/**
 * @param
 * @return
 */
public class ProducerTest {

    public static void main(String[] args) throws JMSException {
        ConnectionFactory connectionFactory=new ActiveMQConnectionFactory("admin","admin","tcp://192.168.241.135:61616");

        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue1108 = session.createQueue("TEST1108");
        MessageProducer producer = session.createProducer(queue1108);
        TextMessage message=new ActiveMQTextMessage();
        message.setText("天气不太好");
        producer.send(message);

        producer.close();
        session.close();
        connection.close();


    }


}
