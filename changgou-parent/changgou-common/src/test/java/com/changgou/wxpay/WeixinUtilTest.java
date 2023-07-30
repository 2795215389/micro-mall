package com.changgou.wxpay;

import com.github.wxpay.sdk.WXPayUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * author:JiangSong
 * Date:2023/7/27
 **/


public class WeixinUtilTest {

    /**
     * 生成随机字符串
     */
    @Test
    public void testDemo() throws Exception {
        String s = WXPayUtil.generateNonceStr();
        System.out.println("随机字符串：" + s);

        // 将Map转为xml字符串
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("id", "No.001");
        dataMap.put("title", "changgou weixin payment");
        dataMap.put("money", "888");
        String xmlStr = WXPayUtil.mapToXml(dataMap);
        System.out.println("Map转XML字符串：\n" + xmlStr);


        // 将Map转成Xml字符串，并生成签名。key为秘钥
        String signatureXmlStr =WXPayUtil.generateSignedXml(dataMap, "changgou");
        System.out.println("Map转xml签名:\n" + signatureXmlStr);


        // xml字符串转为map
        Map<String, String> mapResult = WXPayUtil.xmlToMap(signatureXmlStr);
        System.out.println("xml字符串转map：\n" + mapResult);
    }

}
