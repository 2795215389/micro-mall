package com.changgou.pay.controller;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.pay.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/weixin/pay")
public class WeixinPayController {


    @Autowired
    private WeixinPayService weixinPayService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Environment env;


    /**
     * 创建二维码连接地址返回给前端 生成二维码图片
     *  旧
     * @return
     */
//    @GetMapping("/create/native")
//    public Result<Map> createNative(@RequestParam("outtradeno") String out_trade_no,@RequestParam("totalfee") String total_fee) {
//
//        Map<String, String> resultMap = weixinPayService.createNative(out_trade_no, total_fee);
//
//        return new Result<Map>(true, StatusCode.OK, "二维码连接地址创建成功", resultMap);
//    }

    /**
     * 创建二维码连接地址返回给前端 生成二维码图片
     *  新：包括MQ信息。
     * @return
     */
    @GetMapping("/create/native")
    public Result<Map> createNative(@RequestParam Map<String, String> paramMap) {

        Map<String, String> resultMap = weixinPayService.createNative(paramMap);

        return new Result<Map>(true, StatusCode.OK, "二维码连接地址创建成功", resultMap);
    }

    /**
     * 根据交易订单号 来查询订单的状态
     *
     * @param out_trade_no
     * @return
     */
    @RequestMapping("/status/query")
    public Result<Map> queryStatus(@RequestParam("outtradeno") String out_trade_no) {
        Map<String, String> resultMap = weixinPayService.queryStatus(out_trade_no);
        return new Result<Map>(true, StatusCode.OK, "查询状态OK", resultMap);
    }



    /**
     * 也就是说，向微信发送请求时要设置notificationUrl，设置【域名+该接口url】
     * 经过花生壳转发，就能到达该方法
     * 接收 微信支付通知的结果  结果(以流的形式传递过来)
     */
    @RequestMapping("/notify/url")
    public String notificationResult(HttpServletRequest request) {

        try {

            //1.获取流信息
            ServletInputStream ins = request.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();


            byte[] buffer = new byte[1024];
            int len;
            while ((len = ins.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            bos.close();
            ins.close();

            //2.转换成XML字符串
            byte[] bytes = bos.toByteArray();

            //微信支付系统传递过来的XML的字符串
            String resultStrXML = new String(bytes, "utf-8");
            //3.转成MAP
            Map<String, String> map = WXPayUtil.xmlToMap(resultStrXML);

            System.out.println(resultStrXML);

            //4.发送消息给Rabbitmq  .........
            // 秒杀也会用到MQ。因此不要写死，根据微信回调来投递消息
            String data = JSON.toJSONString(map);
            // rabbitTemplate.convertAndSend(env.getProperty("mq.pay.exchange.order"),env.getProperty("mq.pay.routing.key"),data);

            String attach = map.get("attach");
            Map<String, String> attachMap = JSON.parseObject(attach, Map.class);
            rabbitTemplate.convertAndSend(attachMap.get("exchange"),attachMap.get("routingKey"),data);


            //5.返回微信的接收请况(XML的字符串)

            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("return_code", "SUCCESS");
            resultMap.put("return_msg", "OK");
            return WXPayUtil.mapToXml(resultMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @GetMapping("/close")
    Result closePay(String orderId) throws Exception {
        weixinPayService.closePay(Long.valueOf(orderId));
        return new Result(true, 200, "关闭支付成功！");
    }

}
