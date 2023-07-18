package com.changgou.oauth.util;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 继承了spring security的user，可以自定义添加信息，自定义放到令牌的载荷
 */
public class UserJwt extends User {
    private String id;    //用户ID
    private String name;  //用户名字

    private String company;//公司

    public UserJwt(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
