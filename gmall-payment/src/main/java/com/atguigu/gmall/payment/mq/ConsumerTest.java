package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * @param
 * @return
 */
public class ConsumerTest {

    public static void main(String[] args) throws JMSException {
        ConnectionFactory connectionFactory=new ActiveMQConnectionFactory("admin","admin","tcp://192.168.241.135:61616");

        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        Queue test1108 = session.createQueue("TEST1108");

        MessageConsumer consumer = session.createConsumer(test1108);

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                TextMessage textMessage = (TextMessage) message;
                try {
                    String text = textMessage.getText();
                    System.out.println("text = " + text);
                    textMessage.acknowledge();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });


    }
}
