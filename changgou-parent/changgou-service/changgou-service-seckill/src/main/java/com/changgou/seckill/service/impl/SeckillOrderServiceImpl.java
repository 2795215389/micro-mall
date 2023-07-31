package com.changgou.seckill.service.impl;

import com.changgou.entity.IdWorker;
import com.changgou.entity.SystemConstants;
import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.pojo.SeckillStatus;
import com.changgou.seckill.service.SeckillOrderService;
import com.changgou.seckill.task.MultiThreadingCreateOrder;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;


@Service
@Slf4j
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;


    /**
     * SeckillOrder条件+分页查询
     * @param seckillOrder 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<SeckillOrder> findPage(SeckillOrder seckillOrder, int page, int size){
        //分页
        PageHelper.startPage(page,size);
        //搜索条件构建
        Example example = createExample(seckillOrder);
        //执行搜索
        return new PageInfo<SeckillOrder>(seckillOrderMapper.selectByExample(example));
    }

    /**
     * SeckillOrder分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageInfo<SeckillOrder> findPage(int page, int size){
        //静态分页
        PageHelper.startPage(page,size);
        //分页查询
        return new PageInfo<SeckillOrder>(seckillOrderMapper.selectAll());
    }

    /**
     * SeckillOrder条件查询
     * @param seckillOrder
     * @return
     */
    @Override
    public List<SeckillOrder> findList(SeckillOrder seckillOrder){
        //构建查询条件
        Example example = createExample(seckillOrder);
        //根据构建的条件查询数据
        return seckillOrderMapper.selectByExample(example);
    }


    /**
     * SeckillOrder构建查询对象
     * @param seckillOrder
     * @return
     */
    public Example createExample(SeckillOrder seckillOrder){
        Example example=new Example(SeckillOrder.class);
        Example.Criteria criteria = example.createCriteria();
        if(seckillOrder!=null){
            // 主键
            if(!StringUtils.isEmpty(seckillOrder.getId())){
                    criteria.andEqualTo("id",seckillOrder.getId());
            }
            // 秒杀商品ID
            if(!StringUtils.isEmpty(seckillOrder.getSeckillId())){
                    criteria.andEqualTo("seckillId",seckillOrder.getSeckillId());
            }
            // 支付金额
            if(!StringUtils.isEmpty(seckillOrder.getMoney())){
                    criteria.andEqualTo("money",seckillOrder.getMoney());
            }
            // 用户
            if(!StringUtils.isEmpty(seckillOrder.getUserId())){
                    criteria.andEqualTo("userId",seckillOrder.getUserId());
            }
            // 创建时间
            if(!StringUtils.isEmpty(seckillOrder.getCreateTime())){
                    criteria.andEqualTo("createTime",seckillOrder.getCreateTime());
            }
            // 支付时间
            if(!StringUtils.isEmpty(seckillOrder.getPayTime())){
                    criteria.andEqualTo("payTime",seckillOrder.getPayTime());
            }
            // 状态，0未支付，1已支付
            if(!StringUtils.isEmpty(seckillOrder.getStatus())){
                    criteria.andEqualTo("status",seckillOrder.getStatus());
            }
            // 收货人地址
            if(!StringUtils.isEmpty(seckillOrder.getReceiverAddress())){
                    criteria.andEqualTo("receiverAddress",seckillOrder.getReceiverAddress());
            }
            // 收货人电话
            if(!StringUtils.isEmpty(seckillOrder.getReceiverMobile())){
                    criteria.andEqualTo("receiverMobile",seckillOrder.getReceiverMobile());
            }
            // 收货人
            if(!StringUtils.isEmpty(seckillOrder.getReceiver())){
                    criteria.andEqualTo("receiver",seckillOrder.getReceiver());
            }
            // 交易流水
            if(!StringUtils.isEmpty(seckillOrder.getTransactionId())){
                    criteria.andEqualTo("transactionId",seckillOrder.getTransactionId());
            }
        }
        return example;
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(Long id){
        seckillOrderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 修改SeckillOrder
     * @param seckillOrder
     */
    @Override
    public void update(SeckillOrder seckillOrder){
        seckillOrderMapper.updateByPrimaryKey(seckillOrder);
    }

    /**
     * 增加SeckillOrder
     * @param seckillOrder
     */
    @Override
    public void add(SeckillOrder seckillOrder){
        seckillOrderMapper.insert(seckillOrder);
    }

    /**
     * 根据ID查询SeckillOrder
     * @param id
     * @return
     */
    @Override
    public SeckillOrder findById(Long id){
        return  seckillOrderMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询SeckillOrder全部数据
     * @return
     */
    @Override
    public List<SeckillOrder> findAll() {
        return seckillOrderMapper.selectAll();
    }


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;

    /**
     * 秒杀下单---先排队
     * @param id
     * @param time  时间段
     * @param username
     * @return
     */
    @Override
    public boolean add(Long id, String time, String username) throws UnsupportedEncodingException {


        // Redis服务线程单线程特性：解决用户重复抢单问题;
        Long userQueueCount = redisTemplate.boundHashOps(SystemConstants.SEC_KILL_QUEUE_REPEAT_KEY).increment(username, 1);

        //判断 是否大于1 如果是说明存在未支付订单无法再抢购
        if(userQueueCount>1){
            throw  new RuntimeException("重复排队！");
        }




        /**
         * username 抢单的用户是谁
         * status 1  表示抢单的状态 (1.排队中)
         * id 抢的商品的ID
         * time :抢的商品的所属时间段
         */
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(),1, id,time);

        //进入排队中
        redisTemplate.boundListOps(SystemConstants.SEC_KILL_USER_QUEUE_KEY).leftPush(seckillStatus);
        //维护排队状态： 1:排队中，2:秒杀等待支付,3:支付超时，4:秒杀失败,5:支付完成
        redisTemplate.boundHashOps(SystemConstants.SEC_KILL_USER_STATUS_KEY).put(username,seckillStatus);

        //多线程下单
        multiThreadingCreateOrder.createOrderNew();

        return true;
    }

    @Override
    public SeckillStatus queryStatus(String username) {
      return (SeckillStatus) redisTemplate.boundHashOps(SystemConstants.SEC_KILL_USER_STATUS_KEY).get(username);
    }




    /***
     * 更新订单状态
     * @param out_trade_no
     * @param transaction_id
     * @param username
     */
    @Override
    public void updatePayStatus(String out_trade_no, String transaction_id,String username) {
        //订单数据从Redis数据库查询出来
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps(SystemConstants.SEC_KILL_ORDER_KEY).get(username);
        if (seckillOrder == null) {
            log.warn("order don't exist! out_trade_no = {}, transaction_id = {}, uid = {}",out_trade_no, transaction_id, username);
        }
        //修改状态
        seckillOrder.setStatus("1");

        //支付时间
        seckillOrder.setPayTime(new Date());
        //同步到MySQL中
        seckillOrderMapper.updateByPrimaryKey(seckillOrder);

        //清空Redis缓存，预订单
        redisTemplate.boundHashOps(SystemConstants.SEC_KILL_ORDER_KEY).delete(username);

        //清空用户排队数据，乐观锁
        redisTemplate.boundHashOps(SystemConstants.SEC_KILL_USER_QUEUE_KEY).delete(username);

        //删除抢购状态信息，状态队列
        redisTemplate.boundHashOps(SystemConstants.SEC_KILL_USER_STATUS_KEY).delete(username);
    }

    /***
     * 关闭订单，回滚Redis库存
     * @param username
     */
    @Override
    public void closeOrder(String username) {
        //将消息转换成SeckillStatus
        SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
        //获取Redis中订单信息
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);

        //如果Redis中有订单信息，说明用户未支付
        if(seckillStatus!=null && seckillOrder!=null){
            //删除订单
            redisTemplate.boundHashOps("SeckillOrder").delete(username);
            //回滚库存
            //1)从Redis中获取该商品
            SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).get(seckillStatus.getGoodsId());

            //2)如果Redis中没有，则从数据库中加载
            if(seckillGoods==null){
                seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillStatus.getGoodsId());
            }

            //3)数量+1  (递增数量+1，队列数量+1)
            Long surplusCount = redisTemplate.boundHashOps(SystemConstants.SECK_KILL_GOODS_COUNT_KEY).increment(seckillStatus.getGoodsId(), 1);
            redisTemplate.opsForValue().increment(SystemConstants.SEC_KILL_OVER_SALE_SEMAPHORE+seckillGoods.getId());
            seckillGoods.setStockCount(surplusCount.intValue());

            //4)数据同步到Redis中
            redisTemplate.boundHashOps(SystemConstants.SEC_KILL_GOODS_PREFIX+seckillStatus.getTime()).put(seckillStatus.getGoodsId(),seckillGoods);

            //清理排队标示
            redisTemplate.boundHashOps(SystemConstants.SEC_KILL_USER_QUEUE_KEY).delete(seckillStatus.getUsername());

            //清理抢单标示
            redisTemplate.boundHashOps(SystemConstants.SEC_KILL_USER_STATUS_KEY).delete(seckillStatus.getUsername());
        }
    }
}
