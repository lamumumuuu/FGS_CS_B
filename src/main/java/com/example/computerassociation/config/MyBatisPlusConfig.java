// src/main/java/com/example/computerassociation/config/MyBatisPlusConfig.java

/**
 * MyBatis-Plus 配置类
 * 
 * 主要完成两件事：
 * 1. 通过 @MapperScan 指定 Mapper 接口的包路径，避免在每个 Mapper 上单独加 @Mapper。
 * 2. 注册 MybatisPlusInterceptor Bean，为后续添加分页插件等扩展预留。
 */

package com.example.computerassociation.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.computerassociation.mapper")  // 扫描 Mapper 接口，等同于在每个接口上加 @Mapper
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器
     * 当前返回空拦截器链，后续可按需添加分页插件、乐观锁插件等。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }
}