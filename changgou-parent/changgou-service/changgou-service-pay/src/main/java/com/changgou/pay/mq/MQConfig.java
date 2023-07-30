package com.changgou.pay.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * author:JiangSong
 * Date:2023/7/27
 **/

@Configuration
public class MQConfig {
    @Autowired
    private Environment env;

    //创建队列
    @Bean
    public Queue createQueue(){
        return new Queue(env.getProperty("mq.pay.queue.order"));
    }

    //创建交换机

    @Bean
    public DirectExchange basicExchange(){
        return new DirectExchange(env.getProperty("mq.pay.exchange.order"));
    }

    //绑定

    @Bean
    public Binding basicBinding(){
        return  BindingBuilder.bind(createQueue()).to(basicExchange()).with(env.getProperty("mq.pay.routing.key"));
    }

    /***
     * 创建秒杀队列
     * @return
     */
    @Bean(name = "queueSeckillOrder")
    public Queue queueSeckillOrder(){
        return new Queue(env.getProperty("mq.pay.queue.seckillorder"), true);
    }


    /****
     * 队列绑定到交换机上
     * @return
     */
    @Bean
    public Binding basicBindingSeckillOrder(){
        return BindingBuilder
                .bind(queueSeckillOrder())
                .to(basicExchange())
                .with(env.getProperty("mq.pay.routing.seckillorderkey"));
    }
}
