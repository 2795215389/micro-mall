package com.changgou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;


@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
@EnableEurekaClient
@EnableFeignClients(basePackages = "com.changgou.log.feign")
@EnableElasticsearchRepositories(basePackages = "com.changgou.log.dao")
public class LogApplication {

    /**
     * Springboot整合Elasticsearch 在项目启动前设置一下的属性，防止报错
     * 解决netty冲突后初始化client时还会抛出异常
     * java.lang.IllegalStateException: availableProcessors is already set to [12], rejecting [12]
     ***/
      //  System.setProperty("es.set.netty.runtime.available.processors", "false");
    public static void main(String[] args) {
        SpringApplication.run(LogApplication.class,args);
    }

}
