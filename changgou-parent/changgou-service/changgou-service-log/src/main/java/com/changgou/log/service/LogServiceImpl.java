package com.changgou.log.service;

import com.changgou.log.dao.LogESMapper;
import com.changgou.log.pojo.LogInfo;

import javax.annotation.Resource;



public class LogServiceImpl implements LogService{
    @Resource
    LogESMapper mapper;
    @Override
    public void recordLog(LogInfo logInfo) {
        mapper.save(logInfo);
    }
}
