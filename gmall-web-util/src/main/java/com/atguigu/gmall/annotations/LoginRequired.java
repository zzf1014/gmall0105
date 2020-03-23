package com.atguigu.gmall.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 在方法中有效
@Retention(RetentionPolicy.RUNTIME) // 在jvm中也有效
public @interface LoginRequired {

    boolean loginSuccess() default true; // 为true 必须登陆 false未登陆也可访问
}
