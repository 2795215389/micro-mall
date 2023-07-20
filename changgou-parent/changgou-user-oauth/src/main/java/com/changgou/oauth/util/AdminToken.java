package com.changgou.oauth.util;

import com.alibaba.fastjson.JSON;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

/**
 * author:JiangSong
 * Date:2023/7/20
 **/


public class AdminToken {

    /**
     * 管理员令牌发放
     * @return
     */
    public static String getAdminToken(){
        //证书文件路径
        String key_location="changgou.jks";
        //秘钥库密码
        String key_password="changgou";
        //秘钥密码
        String keypwd = "changgou";
        //秘钥别名
        String alias = "changgou";

        //访问证书路径 读取jks的文件
        ClassPathResource resource = new ClassPathResource(key_location);

        //创建秘钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(resource,key_password.toCharArray());

        //读取秘钥对(公钥、私钥)
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(alias,keypwd.toCharArray());

        //获取私钥 RSA算法
        RSAPrivateKey rsaPrivate = (RSAPrivateKey) keyPair.getPrivate();

        //自定义Payload,创建令牌
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("address", "sz");
        tokenMap.put("name", "itheima");
        tokenMap.put("roles", "ROLE_VIP,ROLE_USER");
        tokenMap.put("authorities", new String[]{"admin", "oauth"});

        //生成Jwt令牌,Spring 继承JWT的api
        Jwt jwt = JwtHelper.encode(JSON.toJSONString(tokenMap), new RsaSigner(rsaPrivate));


        //取出令牌
        String token = jwt.getEncoded();
        return token;
    }
}
