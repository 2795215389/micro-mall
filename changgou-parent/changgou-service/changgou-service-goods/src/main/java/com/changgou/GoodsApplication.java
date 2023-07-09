package com.changgou;

import com.changgou.entity.IdWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
//封装了基本CRUD操作
import org.springframework.context.annotation.Bean;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import tk.mybatis.spring.annotation.MapperScan;



//注意 要使用通用的mapper的组件扫描
@MapperScan(basePackages = {"com.changgou.goods.dao"})
// mapper接口继承了通用的mapper
//默认提供一些方法:
//   insert
//   update

//  delete

//  select
@SpringBootApplication

@EnableEurekaClient
@EnableSwagger2
public class GoodsApplication {
    @Bean
    public IdWorker idWorker(){
        return new IdWorker();
    }

    public static void main(String[] args) {
        SpringApplication.run(GoodsApplication.class, args);
    }
}
