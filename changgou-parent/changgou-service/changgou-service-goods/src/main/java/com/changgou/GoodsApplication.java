package com.changgou;

import com.changgou.entity.IdWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import tk.mybatis.spring.annotation.MapperScan;


// mapper接口继承了通用的mapper
//默认提供一些方法:
//   insert
//   update

//  delete

//  select
//注意 要使用通用的mapper的组件扫描
@MapperScan(basePackages = {"com.changgou.goods.dao"})
@SpringBootApplication
@EnableEurekaClient
@EnableSwagger2
public class GoodsApplication {
    @Bean
    public IdWorker idWorker(){
        return new IdWorker();
    }

    /**
     * feign拦截器,如果加了就会失败，因为并没有启用你feign
     * @return
     */
//    @Bean
//    public FeignInterceptor feignInterceptor() {
//        return new FeignInterceptor();
//    }

    public static void main(String[] args) {
        SpringApplication.run(GoodsApplication.class, args);
    }
}
