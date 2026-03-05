package com.yunpicture.yunpicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;


@MapperScan("com.yunpicture.yunpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
public class yunpicturebackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(yunpicturebackendApplication.class, args);
    }

}
