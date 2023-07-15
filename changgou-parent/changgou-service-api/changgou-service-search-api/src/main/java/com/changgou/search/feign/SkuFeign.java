package com.changgou.search.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * author:JiangSong
 * Date:2023/7/15
 **/

@FeignClient(value="search")
@RequestMapping("/search")
public interface SkuFeign {
    @RequestMapping("/import")
    void importEs();

    @GetMapping
    Map<String, Object> search(@RequestParam(required = false) Map searchMap);
}
