package com.yupi.yupicturebakend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.yupi.yupicturebakend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class YuPictureBakendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuPictureBakendApplication.class, args);
    }

}
