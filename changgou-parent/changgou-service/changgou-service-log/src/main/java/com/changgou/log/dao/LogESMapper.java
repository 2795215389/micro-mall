package com.changgou.log.dao;

import com.changgou.log.pojo.LogInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;




public interface LogESMapper extends ElasticsearchRepository<LogInfo,Long> {

}
