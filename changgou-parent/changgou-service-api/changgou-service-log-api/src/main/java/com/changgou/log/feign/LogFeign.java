package com.changgou.log.feign;

import com.changgou.log.pojo.LogInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;



@FeignClient(value="log")
@RequestMapping("/log")
public interface LogFeign {
    @PostMapping("/record")
    void record(@RequestParam(required = false) LogInfo logInfo);
}
