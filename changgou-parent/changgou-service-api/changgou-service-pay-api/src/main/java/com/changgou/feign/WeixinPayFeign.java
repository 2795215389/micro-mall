package com.changgou.feign;

import com.changgou.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * author:JiangSong
 * Date:2023/7/30
 **/

@FeignClient("/pay")
@RequestMapping("/weixin/pay")
public interface WeixinPayFeign {

    @GetMapping("/close")
    Result closePay(String orderId);
}
