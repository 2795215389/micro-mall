package com.changgou.pay.service;

import java.util.Map;


public interface WeixinPayService {
    /**
     * 关闭支付
     * @param orderId
     * @return
     * @throws Exception
     */
    Map<String, String> closePay(Long orderId) throws Exception;
    /**
     * 生成二维码
     * @param out_trade_no
     * @param total_fee
     * @return
     */
    Map<String,String> createNative(String out_trade_no, String total_fee);

    Map<String,String> createNative(Map<String,String> paramMap);

    /**
     *
     * @param out_trade_no
     * @return
     */
    Map<String,String> queryStatus(String out_trade_no);
}
