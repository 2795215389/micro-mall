package com.changgou.canal;

import com.alibaba.fastjson.JSON;
import com.changgou.content.feign.ContentFeign;
import com.changgou.content.pojo.Content;
import com.changgou.entity.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

import java.util.List;

/**
 * author:JiangSong
 * Date:2023/7/10
 *
 *
 * 业务逻辑，当有canal监听到mysql数据有变更时，
 * 则需要通过feign调用获取到最新数据，然后同步到redis中
 **/
@CanalTable("tb_content")
public class ContentHandler implements EntryHandler<Content> {
    @Autowired
    ContentFeign contentFeign;
    @Autowired
    RedisTemplate redisTemplate;

    private static final String CONTENT_PREFIX = "content_";

    Logger logger = LoggerFactory.getLogger(Content.class);
    @Override
    public void insert(Content content) {
        logger.info("新增content,content = {}", JSON.toJSONString(content));
        Result<List<Content>> byCategory = contentFeign.findByCategory(content.getId());
        redisTemplate.opsForValue().set(CONTENT_PREFIX+content.getId(), byCategory.getData());
    }

    @Override
    public void update(Content before, Content after) {
        logger.info("修改content,before = {}, after = {}",JSON.toJSONString(before), JSON.toJSONString(after));
        delete(before);
        Result<List<Content>> byCategory = contentFeign.findByCategory(after.getId());
        redisTemplate.opsForValue().set(CONTENT_PREFIX+after.getId(), byCategory.getData());
    }

    @Override
    public void delete(Content content) {
        logger.info("删除content,content = {}", JSON.toJSONString(content));
        redisTemplate.delete(CONTENT_PREFIX+content.getId());
    }
}
