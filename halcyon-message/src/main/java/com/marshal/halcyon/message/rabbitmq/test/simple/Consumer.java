package com.marshal.halcyon.message.rabbitmq.test.simple;

import com.marshal.halcyon.message.rabbitmq.test.ConnectionUtils;
import com.rabbitmq.client.*;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @auth: Marshal
 * @date: 2019/7/20
 * @desc:
 */
public class Consumer {

    private static final String QUEUE_NAME = "halcyon_simple_queue";

    public static void main(String[] args) throws Exception {
        //获取连接
        Connection connection = ConnectionUtils.getConnection();
        //获取频道
        Channel channel = connection.createChannel();

        //创建队列声明
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                System.out.println(msg);
            }
        };

        //监听队列
        channel.basicConsume(QUEUE_NAME, true, consumer);

    }
}
