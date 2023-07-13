package com.changgou.search.controller;
import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.search.service.SkuService;
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

    @Autowired
    private SkuService skuService;

    @RequestMapping("/import")
    public Result importEs() {

        skuService.importEs();
        return new Result(true, StatusCode.OK, "导入成功");
    }

    /**
     *
     * @param searchMap  搜索的条件 map
     * @return  resultMap  返回的结果 map
     */
    @PostMapping
    public Map search(@RequestBody(required = false) Map searchMap){
        Object pageNum = searchMap.get("pageNum");
        if(pageNum==null){
            searchMap.put("pageNum","1");
        }
        if(pageNum instanceof Integer){
            searchMap.put("pageNum",pageNum.toString());
        }
        return skuService.search(searchMap);
    }
}
