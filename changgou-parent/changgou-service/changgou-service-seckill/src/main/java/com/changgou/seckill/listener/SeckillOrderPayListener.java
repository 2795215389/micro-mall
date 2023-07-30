package com.changgou.seckill.listener;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.service.SeckillOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 监听秒杀的队列
 *
 */
@Component
@RabbitListener(queues = "${mq.pay.queue.seckillorder}")
public class SeckillOrderPayListener {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private SeckillOrderService seckillOrderService;

    @RabbitHandler
    public void consumer(String message) throws Exception{
        System.out.println("消费端接收数据======="+ new Date());
        //获取数据 json格式的字符串
        Map<String,String> allMap = JSON.parseObject(message, Map.class);
        //判断 是否成功(通信是否成)
        if(allMap!=null) {
            String return_code = allMap.get("return_code");
            if("SUCCESS".equalsIgnoreCase(return_code)) {
                String result_code = allMap.get("result_code");
                String attach = allMap.get("attach");//json格式的字符串 (里面有用户名信息)
                Map<String,String> attachMap = JSON.parseObject(attach, Map.class);

                String username = attachMap.get("username");
                String outTradeNo = allMap.get("out_trade_no");
                String trx_id = allMap.get("transaction_id");

                // 订单同步到MySQL，失败需要回滚Redis库存
                if("SUCCESS".equalsIgnoreCase(result_code)) {
                    seckillOrderService.updatePayStatus(outTradeNo, trx_id, username);
                }else{
                    seckillOrderService.closeOrder(username);
                }
            }
        }
    }
}
