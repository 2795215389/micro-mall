package com.changgou.search.controller;
import com.alibaba.fastjson.JSON;
import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.search.service.SkuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * controller
 * 用于接收页面传递的请求 来测试 导入数据
 * 实现搜索的功能
 *
 */
@RestController
@CrossOrigin
@RequestMapping("/search")
public class SkuController {

    Logger log = LoggerFactory.getLogger(SkuController.class);
    @Autowired
    private SkuService skuService;

    /**
     * 将数据导入ES
     * @return
     */
    @GetMapping("/import")
    public Result importEs() {

        skuService.importEs();
        return new Result(true, StatusCode.OK, "导入成功");
    }

    /**
     *
     * @param searchMap  搜索的条件 map
     * @return  resultMap  返回的结果 map
     */
    @GetMapping
    public Map<String, Object> search(@RequestParam(required = false) Map searchMap){
        log.info("searchMap:{}", JSON.toJSONString(searchMap));
        return skuService.search(searchMap);
    }



}
