package com.changgou.log.aop;


import java.lang.annotation.*;

@Target({ElementType.METHOD})//能放在什么对象前
@Retention(RetentionPolicy.RUNTIME)//在运行时起作用
@Documented//能够外加文档
public @interface SystemLog {
    //日志名称,行为描述
    String description() default "";
    //日志类型
    LogType type();
}
