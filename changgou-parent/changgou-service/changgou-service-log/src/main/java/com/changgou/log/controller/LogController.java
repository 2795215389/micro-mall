package com.changgou.log.controller;

import com.changgou.log.service.LogService;
import com.changgou.log.pojo.LogInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;



@RestController
@CrossOrigin
@RequestMapping("/log")
public class LogController {
    @Resource
    private LogService logService;
    @PostMapping("/record")
    public void record(@RequestBody LogInfo logInfo) {
        try {
            logService.recordLog(logInfo);
        } catch (Exception e) {
            System.out.println("log record failed!");
        }
    }
}
