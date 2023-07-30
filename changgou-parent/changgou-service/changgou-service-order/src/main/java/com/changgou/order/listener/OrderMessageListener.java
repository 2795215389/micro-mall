package com.changgou.order.listener;

import com.alibaba.fastjson.JSON;
import com.changgou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * author:JiangSong
 * Date:2023/7/27
 **/

@Component
@RabbitListener(queues = "${mq.pay.queue.order}")
@Slf4j
public class OrderMessageListener {

    @Resource
    private OrderService orderService;
    /**
     * 支付结果监听
     */
    @RabbitHandler
    public void handleMsg(String message){
        Map<String, String> msgMap = JSON.parseObject(message, Map.class);
        if (msgMap == null) {
            log.warn("msgMap is null");
            return;
        }
        // 通信标识
        String returnCode = msgMap.get("return_code");
        if (returnCode.equals("SUCCESS")) {
            // 业务结果
            String resultCode = msgMap.get("result_code");
            String outTradeNo = msgMap.get("out_trade_no");
            // 改变状态，若失败则回滚
            if (resultCode.equals("SUCCESS")) {
                //修改订单状态  out_trade_no
                orderService.updateStatus(outTradeNo,msgMap.get("transaction_id"));
            } else {
                //支付失败，关闭支付【微信】，取消订单【订单状态】，回滚库存【feign--goods】
            }
        }

    }
}
