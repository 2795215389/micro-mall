package com.changgou.token;

import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

import java.util.Base64;


public class ParseJwtTest {

    /***
     * 校验令牌
     */
    @Test
    public void testParseToken(){
        //令牌
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZGRyZXNzIjoic3oiLCJyb2xlcyI6IlJPTEVfVklQLFJPTEVfVVNFUiIsIm5hbWUiOiJpdGhlaW1hIn0.r6ZtcVx51SBNpi6bh_JHHsPq4Bbn841Qoat25_aS-8xN3GaPwiS0xgKzo3a96On9k5s9Er0t-zGDlaWBwcpfTnx19iCADCoj2hwh_Bq7K2Z0eOqMByZyMxSwBwaqqixwD3kCYwJYd2Hr257RFWaEsXJYq9TJGh2kXw9HHA4gIuV71FuQA7L4fghOpYPvIjtzk0By1VfdgbGaRYF07XhWmRFbjpdoT-a9iE80qwQQdZIM_ruQbJMntHmjuISSTCA-7EqamQMcGrp1DfJH7R17Ksl-DUWa5aDDK9dhl6ABcwbXdjZtz6wQ5ujYMJ1sohTYS34HhM9en-LK2PZw09HTHQ";
        //公钥
        String publickey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvf5mu0ER+6WWwdob1QsMLpm+fMYDKFxNCRyoQntzD268oVjOLFJT8DJYB7f6BLYieA1UrnLzVBMSJkEKx5N4lIckU7uXgKwR+WjIE4tp7YU1MfmMKdNJ/M7foopjMdd6WMd/D36vc3DChRlMVIamMJOyaMas1c96UOpNj8f2rsRKmS9Bszvi6Jv9rCnmRlaCfb2MIEsb5rWaV6+a5b8kiGIFw9dAs7c/lQGZcHFLTOPxRy4brZQQSkmrfxpkWb6Uge2cObMBe8A6WCeZ9dKQyu4KInNag8ed38okddnDUDfmS3/LTwYaioK9IVznaL/mob6gqANl8OFQh8nX1aIZ5wIDAQAB-----END PUBLIC KEY-----";
        //校验Jwt
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(publickey));

        //获取Jwt原始内容 载荷
        String claims = jwt.getClaims();
        System.out.println(claims);
        //jwt令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);
    }


    /**
     *  basic Authorization其实就是在头部加上了客户端信息
     *  解出来就是，      changgou：changgou
     */
    @Test
    public void testBase64(){
        String str = "Y2hhbmdnb3U6Y2hhbmdnb3U=";
        byte[] decode = Base64.getDecoder().decode(str);
        String decodeStr = new String(decode);
        System.out.println(decodeStr);
    }
}
