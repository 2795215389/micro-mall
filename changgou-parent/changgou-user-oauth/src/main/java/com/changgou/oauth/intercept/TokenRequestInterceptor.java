package com.changgou.oauth.intercept;

import com.changgou.oauth.util.AdminToken;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;

/**
 * author:JiangSong
 * Date:2023/7/20
 *
 *
 * Feign拦截器，在执行之前将管理员Token封装进头信息
 **/

@Configuration
public class TokenRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // 生成管理员令牌
        String token = AdminToken.getAdminToken();
        template.header("Authorization", "bearer "+token);
    }
}
