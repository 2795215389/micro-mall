package com.changgou.seckill.timer;

import com.changgou.entity.DateUtil;
import com.changgou.entity.SystemConstants;
import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 定时任务 spring task(多线程)    spring schedule
 * 1.开始spring task
 * 2.在执行的方法上修饰一个注解  注解中指定何时执行即可
 *
 * 定时将秒杀商品存入Redis缓存
 */
@Component
public class SeckillGoodsPushTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    //反复被执行的方法 隔5秒钟执行一次
    // 在现实中应该在CPU和内存使用低峰，比如凌晨2点执行
    //@Scheduled(cron = "0/* * 2 * * ?")
    @Scheduled(cron = "0/5 * * * * ?")
    public void loadGoodsPushRedis() {
        //1.获取当前的时间对应的5个时间段,当前时间段为区间开始
        List<Date> dateMenus = DateUtil.getDateMenus();
        //2.循环遍历5个时间段 获取到时间的日期
        for (Date starttime : dateMenus) {

            //20230729[10]
            String extName =  DateUtil.data2str(starttime,DateUtil.PATTERN_YYYYMMDDHH);
            //3.将循环到的时间段作为条件 从数据库中执行查询 得出数据集

            /**
             * select * from tb_seckill_goods where
             stock_count>0
             and `status`='1'
             and start_time > 开始时间段
             and end_time < 开始时间段+2hour  and id  not in (redis中已有的id)
             */
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("status","1");
            criteria.andGreaterThan("stockCount",0);
            criteria.andGreaterThanOrEqualTo("startTime",starttime);
            criteria.andLessThan("endTime",DateUtil.addDateHour(starttime, 2));

            //排除掉redis已有的商品，不是幂等操作，因为秒杀会删除Redis
            Set keys = redisTemplate.boundHashOps(SystemConstants.SEC_KILL_GOODS_PREFIX + extName).keys();
            if(keys!=null && keys.size()>0) {
                criteria.andNotIn("id", keys);
            }

            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);

            //4.将数据集存储到redis中(key field value的数据格式 )
            for (SeckillGoods seckillGood : seckillGoods) {
                redisTemplate.boundHashOps(SystemConstants.SEC_KILL_GOODS_PREFIX + extName).put(seckillGood.getId(),seckillGood);
                //设置有效期
                redisTemplate.expireAt(SystemConstants.SEC_KILL_GOODS_PREFIX + extName,DateUtil.addDateHour(starttime, 2));

                //超卖问题，用队列维护商品数量，信号量
                redisTemplate.opsForValue().set(SystemConstants.SEC_KILL_OVER_SALE_SEMAPHORE+seckillGood.getId(), seckillGood.getStockCount());
                // pushGoods(seckillGood);

                //添加一个计数器 (key:商品的ID  value : 库存数)
                redisTemplate.boundHashOps(SystemConstants.SECK_KILL_GOODS_COUNT_KEY).increment(seckillGood.getId(),seckillGood.getStockCount());


            }



        }
    }

    public void pushGoods(SeckillGoods seckillGoods){
        //创建redis的队列(每一种商品就是一个队列,队列的元素的个数和商品的库存一致) 压入队列
        for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {//5
                redisTemplate.boundListOps(SystemConstants.SEC_KILL_CHAOMAI_LIST_KEY_PREFIX+seckillGoods.getId()).leftPush(seckillGoods.getId());
        }


    }

    /*public static void main(String[] args) {
        //获取所有的时间段(根据当前的时间获取5个)
        List<Date> dateMenus = DateUtil.getDateMenus();

        for (Date dateMenu : dateMenus) {
            System.out.println(dateMenu);
        }
    }*/
}
